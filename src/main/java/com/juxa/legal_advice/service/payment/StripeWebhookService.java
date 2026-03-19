package com.juxa.legal_advice.service.payment;

import com.juxa.legal_advice.model.PaymentHistoryEntity;
import com.juxa.legal_advice.model.PlanEntity;
import com.juxa.legal_advice.model.SubscriptionEntity;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.PaymentHistoryRepository;
import com.juxa.legal_advice.repository.PlanRepository;
import com.juxa.legal_advice.repository.SubscriptionRepository;
import com.juxa.legal_advice.repository.UserRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlanRepository planRepository;

    public String handleWebhook(String payload, String sigHeader) {
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            log.info("🔔 Webhook recibido exitosamente: Tipo de evento = {}", event.getType());
        } catch (SignatureVerificationException e) {
            log.error("❌ Error de firma en Webhook: Alguien intentó mandar una petición falsa");
            throw new RuntimeException("Firma de Stripe inválida");
        }

        StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);
        if (stripeObject == null) {
            log.warn("⚠️ No se pudo deserializar el objeto Stripe del evento.");
            return "Objeto nulo ignorado";
        }

        try {
            switch (event.getType()) {
                case "checkout.session.completed":
                    log.info("🚀 Procesando checkout.session.completed...");
                    handleCheckoutSessionCompleted((Session) stripeObject);
                    return "Suscripción inicial guardada";

                case "invoice.paid":
                    log.info("💰 Procesando invoice.paid...");
                    handleInvoicePayment((Invoice) stripeObject, true);
                    return "Pago exitoso registrado";

                case "invoice.payment_failed":
                    log.error("🚫 Procesando invoice.payment_failed...");
                    handleInvoicePayment((Invoice) stripeObject, false);
                    return "Fallo de pago registrado";

              //  case "customer.subscription.deleted":
                case "customer.subscription.updated":
                    log.info("🔄 Procesando actualización/eliminación de suscripción...");
                    handleSubscriptionUpdate((Subscription) stripeObject);
                    return "Estado de suscripción actualizado";

                default:
                    log.debug("ℹ️ Evento ignorado (No nos interesa procesarlo): {}", event.getType());
                    return "Evento recibido pero no manejado: " + event.getType();
            }
        } catch (Exception e) {
            log.error("❌ Error crítico procesando evento {}: {}", event.getType(), e.getMessage(), e);
            return e.toString();
        }
    }

    private void handleCheckoutSessionCompleted(Session session) throws Exception {
        String stripeSubId = session.getSubscription();
        if (stripeSubId == null) {
            log.warn("⚠️ El checkout se completó, pero no traía un ID de suscripción. ¿Fue un pago único?");
            return;
        }

        // =================================================================
        // CORRECCIÓN CLAVE: Extraer metadata de la SESIÓN, no de la suscripción
        // Porque en PaymentService usamos putSubscriptionData que a veces tarda
        // en propagarse, pero la Session siempre lo tiene en su metadata si se mandó ahí.
        // Si no está en Session, lo buscamos en la Suscripción.
        // =================================================================
        Subscription stripeSub = Subscription.retrieve(stripeSubId);

        String userIdStr = null;
        String planIdStr = null;

        // Primero intentamos buscar en la metadata de la suscripción (lo ideal)
        if (stripeSub.getMetadata() != null && stripeSub.getMetadata().containsKey("user_id")) {
            userIdStr = stripeSub.getMetadata().get("user_id");
            planIdStr = stripeSub.getMetadata().get("plan_id");
            log.info("✅ Metadata encontrada en la Suscripción. user_id: {}, plan_id: {}", userIdStr, planIdStr);
        }
        // Si no está ahí, intentamos buscar en la metadata de la sesión como plan B
        else if (session.getMetadata() != null && session.getMetadata().containsKey("user_id")) {
            userIdStr = session.getMetadata().get("user_id");
            planIdStr = session.getMetadata().get("plan_id");
            log.info("✅ Metadata encontrada en la Sesión. user_id: {}, plan_id: {}", userIdStr, planIdStr);
        }

        if (userIdStr == null || planIdStr == null) {
            log.error("❌ GRAVE: No se encontró metadata (user_id/plan_id) ni en la sesión ni en la suscripción.");
            throw new RuntimeException("Metadata faltante para asignar la suscripción");
        }

        Long userId = Long.parseLong(userIdStr);
        Long planId = Long.parseLong(planIdStr);

        log.info("🔍 Buscando al usuario con ID {} en la base de datos...", userId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado en BD para el ID: " + userId));

        log.info("🔍 Buscando el plan con ID {} en la base de datos...", planId);
        PlanEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado en BD para el ID: " + planId));

        // 1. Actualizamos el Customer ID en el Usuario
        log.info("👤 Asignando Stripe Customer ID ({}) al usuario {}", session.getCustomer(), user.getEmail());
        user.setStripeCustomerId(session.getCustomer());
        user.setSubscriptionPlan(plan.getName());
        userRepository.save(user);

        // 2. Revisamos si ya existe una suscripción
        Optional<SubscriptionEntity> existingSub = subscriptionRepository.findByUserId(userId);
        SubscriptionEntity subToSave;

        LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeSub.getStartDate()), ZoneId.systemDefault());
        // LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeSub.getEndedAt()), ZoneId.systemDefault());
        Long periodEndEpoch = stripeSub.getItems().getData().get(0).getCurrentPeriodEnd();
        LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochSecond(periodEndEpoch), ZoneId.systemDefault());
        ;

        ;

        if (existingSub.isPresent()) {
            log.info("⬆️ El usuario ya tenía una suscripción. Actualizando (Upgrade/Downgrade)...");
            subToSave = existingSub.get();
            subToSave.setPlan(plan);
            subToSave.setStripeSubscriptionId(stripeSubId);
            subToSave.setStatus(stripeSub.getStatus());
            subToSave.setCurrentPeriodStart(start);
            subToSave.setCurrentPeriodEnd(end);
        } else {
            log.info("✨ Creando nueva suscripción para el usuario...");
            subToSave = SubscriptionEntity.builder()
                    .user(user)
                    .plan(plan)
                    .stripeSubscriptionId(stripeSubId)
                    .status(stripeSub.getStatus())
                    .currentPeriodStart(start)
                    .currentPeriodEnd(end)
                    .build();
        }

        subscriptionRepository.save(subToSave);
        log.info("🎉 ¡ÉXITO! Suscripción {} guardada en DB para el usuario {}", stripeSubId, user.getEmail());
    }

    private void handleInvoicePayment(Invoice invoice, boolean isSuccess) {
        log.info("=========================================================");
        log.info("🚨 INICIANDO DEPURACIÓN DE FACTURA (INVOICE.PAID) 🚨");
        log.info("📦 CONTENIDO COMPLETO DE LA FACTURA DESDE STRIPE:\n{}", invoice.toJson());

        log.info("👉 1. Valor directo de invoice.getSubscription(): {}", invoice.getRawJsonObject());

        if (invoice.getParent() != null) {
            log.info("👉 2. El Parent existe. Tipo de Parent: {}", invoice.getParent().getType());
        } else {
            log.info("👉 2. El Parent es NULL");
        }
        log.info("=========================================================");

        String subscriptionId;
        if (invoice.getParent() != null && "subscription_details".equals(invoice.getParent().getType())) {
            subscriptionId = invoice.getParent().getSubscriptionDetails().getSubscription();
        } else {
            subscriptionId = null;
        }

        if (subscriptionId == null) {
            // Lo cambié a log.warn para asegurarnos de que lo veas en la consola
            log.warn("⚠️ Factura ignorada porque subscriptionId se evaluó como NULL. Revisa la estructura en el JSON de arriba.");
            return;
        }

        log.info("🔍 Buscando suscripción {} para adjuntarle la factura...", subscriptionId);

        subscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .ifPresentOrElse(subEntity -> {
                    BigDecimal amount = BigDecimal.valueOf(invoice.getAmountPaid()).divide(BigDecimal.valueOf(100));

                    if (!isSuccess && amount.compareTo(BigDecimal.ZERO) == 0) {
                        amount = BigDecimal.valueOf(invoice.getAmountDue()).divide(BigDecimal.valueOf(100));
                    }

                    PaymentHistoryEntity payment = PaymentHistoryEntity.builder()
                            .subscription(subEntity)
                            .stripeInvoiceId(invoice.getId())
                            .amountPaid(amount)
                            .status(invoice.getStatus())
                            .build();

                    paymentHistoryRepository.save(payment);
                    log.info("✅ Historial de pago guardado. Factura: {}, Status: {}, Monto: ${}",
                            invoice.getId(), invoice.getStatus(), amount);

                }, () -> {
                    log.error("⚠️ La suscripción {} aún no existe en DB. Lanzando error para forzar reintento de Stripe.", subscriptionId);
                    throw new RuntimeException("Suscripción no encontrada. Forzando reintento del webhook invoice.paid");
                });
    }


 /*   private void handleSubscriptionUpdate(Subscription stripeSub) {
        log.info("🔄 Actualizando status de la suscripción {}...", stripeSub.getId());
        subscriptionRepository.findByStripeSubscriptionId(stripeSub.getId())
                .ifPresentOrElse(subEntity -> {
                    subEntity.setStatus(stripeSub.getStatus());

                    if ("canceled".equals(stripeSub.getStatus()) || "unpaid".equals(stripeSub.getStatus())) {
                        subEntity.setCurrentPeriodEnd(LocalDateTime.now());
                        log.info("🛑 Suscripción cancelada. Fecha de expiración forzada a hoy.");
                    } else {
                        // este es el metodo correcto para sacar el final del periodo de la suscripción, no lo cambies
                        Long periodEndEpoch = stripeSub.getItems().getData().get(0).getCurrentPeriodEnd();
                        LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochSecond(periodEndEpoch), ZoneId.systemDefault());                    }

                    subEntity.setCancelAtPeriodEnd(stripeSub.getCancelAtPeriodEnd());
                    subscriptionRepository.save(subEntity);
                    log.info("✅ Suscripción actualizada en DB a status: {}", stripeSub.getStatus());
                }, () -> log.warn("⚠️ Intentamos actualizar la suscripción {}, pero no existe en DB.", stripeSub.getId()));
    }
}*/
 private void handleSubscriptionUpdate(Subscription eventSub) {
     log.info("🔄 Actualizando status de la suscripción {}...", eventSub.getId());

     try {
         Subscription stripeSub = Subscription.retrieve(eventSub.getId());

         log.info("👉 Status fresco extraído: {}", stripeSub.getStatus());
         log.info("👉 CancelAtPeriodEnd fresco extraído: {}", stripeSub.getCancelAtPeriodEnd());
         log.info("👉 CancelAt (fecha) fresco extraído: {}", stripeSub.getCancelAt());

         subscriptionRepository.findByStripeSubscriptionId(stripeSub.getId())
                 .ifPresentOrElse(subEntity -> {
                     subEntity.setStatus(stripeSub.getStatus());

                     if ("canceled".equals(stripeSub.getStatus()) || "unpaid".equals(stripeSub.getStatus())) {
                         subEntity.setCurrentPeriodEnd(LocalDateTime.now());
                         log.info("🛑 Suscripción cancelada. Fecha de expiración forzada a hoy.");
                     } else {
                         Long periodEndEpoch = stripeSub.getItems().getData().get(0).getCurrentPeriodEnd();
                         LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochSecond(periodEndEpoch), ZoneId.systemDefault());
                         subEntity.setCurrentPeriodEnd(end);
                     }

                     // ==========================================
                     // LA SOLUCIÓN: Revisar tanto el booleano como la fecha
                     // ==========================================
                     boolean hasBooleanCancel = stripeSub.getCancelAtPeriodEnd() != null && stripeSub.getCancelAtPeriodEnd();
                     boolean hasDateCancel = stripeSub.getCancelAt() != null;

                     // Si cualquiera de los dos es verdadero, la suscripción se cancelará
                     boolean isScheduledToCancel = hasBooleanCancel || hasDateCancel;

                     subEntity.setCancelAtPeriodEnd(isScheduledToCancel);
                     // ==========================================

                     subscriptionRepository.save(subEntity);
                     log.info("✅ Suscripción actualizada en DB a status: {} y cancelAtPeriodEnd: {}",
                             subEntity.getStatus(), subEntity.isCancelAtPeriodEnd());
                 }, () -> log.warn("⚠️ Intentamos actualizar la suscripción {}, pero no existe en DB.", stripeSub.getId()));

     } catch (Exception e) {
         log.error("❌ Error al recuperar la suscripción actualizada de Stripe: {}", e.getMessage());
         throw new RuntimeException("Error al actualizar la suscripción", e);
     }
 }
}