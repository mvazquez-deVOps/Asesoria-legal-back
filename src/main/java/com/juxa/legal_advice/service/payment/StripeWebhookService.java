package com.juxa.legal_advice.service.payment;

import com.juxa.legal_advice.config.exceptions.auth.ResourceNotFoundException;
import com.juxa.legal_advice.config.exceptions.payment.InvalidStripePayloadException;
import com.juxa.legal_advice.config.exceptions.payment.InvalidWebhookSignatureException;
import com.juxa.legal_advice.config.exceptions.payment.WebhookSyncException;
import com.juxa.legal_advice.model.*;
import com.juxa.legal_advice.repository.*;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookService {


    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private final ProcessedStripeEventRepository processedStripeEventRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PlanUsageRepository planUsageRepository;


    public String handleWebhook(String payload, String sigHeader) throws StripeException {
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            // GCP capturará la causa original
            throw new InvalidWebhookSignatureException("Firma de Stripe inválida", e);
        }

        String eventId = event.getId();
        if (processedStripeEventRepository.existsById(eventId)) {
            log.info("[IDEMPOTENCIA] El evento {} ({}) ya fue procesado. Ignorando.", eventId, event.getType());
            return "Evento duplicado ignorado exitosamente";
        }

        processedStripeEventRepository.save(ProcessedStripeEventEntity.builder().id(eventId).build());
        //log.info("Evento {} registrado para evitar duplicados.", eventId);

        StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);
        if (stripeObject == null) {
            log.warn("No se pudo deserializar el objeto Stripe del evento {}.", eventId);
            return "Objeto nulo ignorado";
        }

        switch (event.getType()) {
            case "checkout.session.completed":
                Session session = (Session) stripeObject;
                String paymentStatus = session.getPaymentStatus();

                if ("paid".equals(paymentStatus) || "no_payment_required".equals(paymentStatus)) {
                    Map<String, String> metadata = session.getMetadata();
                    if (metadata != null && "extra_tokens".equals(metadata.get("payment_type"))) {
                        handleExtraTokensPurchase(metadata);
                        return "Compra de tokens procesada exitosamente";
                    } else {
                        handleCheckoutSessionCompleted(session);
                        return "Suscripción inicial guardada";
                    }
                } else {
                    log.warn("Checkout {} completado sin pago válido. Status: {}", session.getId(), paymentStatus);
                    return "Checkout completado sin pago válido";
                }

            case "invoice.paid":
                handleInvoicePayment((Invoice) stripeObject, true);
                return "Pago exitoso registrado";

            case "invoice.payment_failed":
                handleInvoicePayment((Invoice) stripeObject, false);
                return "Fallo de pago registrado";

            case "customer.subscription.deleted":
            case "customer.subscription.updated":
                handleSubscriptionUpdate((Subscription) stripeObject);
                return "Estado de suscripción actualizado";

            default:
                log.debug("Evento recibido pero no manejado: {}", event.getType());
                return "Evento recibido pero no manejado: " + event.getType();
        }
    }

    private void handleCheckoutSessionCompleted(Session session) throws StripeException {
        String stripeSubId = session.getSubscription();
        if (stripeSubId == null) {
            log.warn("El checkout {} se completó sin ID de suscripción.", session.getId());
            return;
        }

        Subscription stripeSub = Subscription.retrieve(stripeSubId);
        String userIdStr = null;
        String planIdStr = null;

        if (stripeSub.getMetadata() != null && stripeSub.getMetadata().containsKey("user_id")) {
            userIdStr = stripeSub.getMetadata().get("user_id");
            planIdStr = stripeSub.getMetadata().get("plan_id");
            log.debug("Metadata encontrada en la Suscripción {}.", stripeSubId);
        } else if (session.getMetadata() != null && session.getMetadata().containsKey("user_id")) {
            userIdStr = session.getMetadata().get("user_id");
            planIdStr = session.getMetadata().get("plan_id");
            log.debug("Metadata encontrada en la Sesión {}.", session.getId());
        }

        if (userIdStr == null || planIdStr == null) {
            throw new InvalidStripePayloadException("Metadata faltante (user_id/plan_id) para asignar la suscripción.");
        }

        Long userId = Long.parseLong(userIdStr);
        Long planId = Long.parseLong(planIdStr);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new WebhookSyncException("Usuario no encontrado en BD para el ID: " + userId));

        PlanEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new WebhookSyncException("Plan no encontrado en BD para el ID: " + planId));

        user.setStripeCustomerId(session.getCustomer());
        user.setSubscriptionPlan(plan.getName());

        applyTrialStatus(stripeSub, user);
        userRepository.save(user);

        Optional<SubscriptionEntity> existingSub = subscriptionRepository.findByUserId(userId);
        SubscriptionEntity subToSave;

        LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeSub.getStartDate()), ZoneId.systemDefault());
        Long periodEndEpoch = stripeSub.getItems().getData().get(0).getCurrentPeriodEnd();
        LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochSecond(periodEndEpoch), ZoneId.systemDefault());

        if (existingSub.isPresent()) {
            subToSave = existingSub.get();
            subToSave.setPlan(plan);
            subToSave.setStripeSubscriptionId(stripeSubId);
            subToSave.setStatus(stripeSub.getStatus());
            subToSave.setCurrentPeriodStart(start);
            subToSave.setCurrentPeriodEnd(end);
        } else {
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
        log.info("Suscripción {} guardada en DB para el usuario {}", stripeSubId, user.getEmail());
    }

    private void handleInvoicePayment(Invoice invoice, boolean isSuccess) {
//         log.info("=========================================================");
//         log.info("🚨 [PASO 1] INICIANDO PROCESAMIENTO DE FACTURA 🚨");
//         log.info("👉 ID de Factura: {}", invoice.getId());

        String subscriptionId = null;

        // Intentamos extraer el subscription ID de las líneas de la factura
        //Si no encontramos la metadata en la session entonces lo buscamos en la subscription
        if (invoice.getLines() != null && invoice.getLines().getData() != null && !invoice.getLines().getData().isEmpty()) {
            subscriptionId = invoice.getLines().getData().get(0).getSubscription();
//             log.info("👉 [PASO 2] Buscando ID en Line Items. Resultado: {}", subscriptionId);

            if (subscriptionId == null && invoice.getLines().getData().get(0).getParent() != null &&
                    invoice.getLines().getData().get(0).getParent().getSubscriptionItemDetails() != null) {
                subscriptionId = invoice.getLines().getData().get(0).getParent().getSubscriptionItemDetails().getSubscription();
//                 log.info("👉 [PASO 2.1] Buscando ID en Parent details. Resultado: {}", subscriptionId);
            }
        } else {
//             log.warn("⚠️ [PASO 2] La factura no tiene Line Items (getLines() es null o vacío)");
        }

        if (subscriptionId == null) {
//             log.warn("🛑 [PASO 4 - ABORTO] Factura ignorada porque no se encontró ID de suscripción por ningún lado.");
//             log.info("=========================================================");
            return;
        }

        if (paymentHistoryRepository.existsByStripeInvoiceId(invoice.getId())) {
//             log.info("♻️ [IDEMPOTENCIA] La factura {} ya fue procesada anteriormente. Ignorando evento duplicado.", invoice.getId());
            return; // Salimos sin lanzar error para que Stripe reciba su 200 OK
        }

//         log.info("🔍 [PASO 5] Buscando suscripción {} en la Base de Datos...", subscriptionId);

        final String finalSubscriptionId = subscriptionId;

        subscriptionRepository.findByStripeSubscriptionId(finalSubscriptionId)
                .ifPresentOrElse(subEntity -> {
//                     log.info("✅ [PASO 6 - ÉXITO] Suscripción encontrada en BD (ID Interno: {}).", subEntity.getId());

                    //  Solo hacemos esto para que los datos ingresen correctamente a la base de datos
                    BigDecimal amount = BigDecimal.valueOf(invoice.getAmountPaid()).divide(BigDecimal.valueOf(100));

                    if (!isSuccess && amount.compareTo(BigDecimal.ZERO) == 0) {
                        amount = BigDecimal.valueOf(invoice.getAmountDue()).divide(BigDecimal.valueOf(100));
                    }

                    String finalStatus = invoice.getStatus() != null ? invoice.getStatus() : (isSuccess ? "paid" : "failed");

                    // 2. Imprimir los datos exactos que vamos a meter a la BD
//                     log.info("👉 [PASO 6.1] VERIFICACIÓN DE DATOS ANTES DE GUARDAR:");
//                     log.info("   - subscription_id (FK): {}", subEntity.getId());
//                     log.info("   - stripe_invoice_id: {}", invoice.getId());
//                     log.info("   - amount_paid: {}", amount);
//                     log.info("   - status: {}", finalStatus);

                    // 3. Construir entidad
                    PaymentHistoryEntity payment = PaymentHistoryEntity.builder()
                            .subscription(subEntity)
                            .stripeInvoiceId(invoice.getId())
                            .amountPaid(amount)
                            .status(finalStatus)
                            .build();

                    // 4. Guardar y FORZAR el insert inmediato en la BD
//                     log.info("👉 [PASO 6.2] Ejecutando saveAndFlush()...");
                    payment = paymentHistoryRepository.saveAndFlush(payment);

//                     log.info("🎉 [FINAL] ¡PaymentHistory insertado correctamente! ID generado en la BD: {}", payment.getId());
//                     log.info("=========================================================");

                }, () -> {
//                     log.error("❌ [PASO 6 - ERROR] La suscripción {} AÚN NO EXISTE en DB.", finalSubscriptionId);
//                     log.error("💡 EXPLICACIÓN: Stripe mandó 'invoice.paid' ANTES de que se guardara la suscripción.");
//                     log.info("=========================================================");
                    throw new WebhookSyncException("Suscripción " + finalSubscriptionId + " no encontrada en DB. Forzando reintento del webhook.");                });
    }

    private void handleSubscriptionUpdate(Subscription eventSub) throws StripeException {
        Subscription stripeSub = Subscription.retrieve(eventSub.getId());

        SubscriptionEntity subEntity = subscriptionRepository.findByStripeSubscriptionId(stripeSub.getId())
                .orElseThrow(() -> new WebhookSyncException("Intento de actualizar suscripción " + stripeSub.getId() + " pero no existe en DB."));

        subEntity.setStatus(stripeSub.getStatus());

        if ("canceled".equals(stripeSub.getStatus()) || "unpaid".equals(stripeSub.getStatus())) {
            subEntity.setCurrentPeriodEnd(LocalDateTime.now());
            log.info("Suscripción {} cancelada/unpaid. Fecha de expiración forzada a hoy.", stripeSub.getId());
        } else {
            Long periodEndEpoch = stripeSub.getItems().getData().get(0).getCurrentPeriodEnd();
            LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochSecond(periodEndEpoch), ZoneId.systemDefault());
            subEntity.setCurrentPeriodEnd(end);
        }

        boolean hasBooleanCancel = stripeSub.getCancelAtPeriodEnd() != null && stripeSub.getCancelAtPeriodEnd();
        boolean hasDateCancel = stripeSub.getCancelAt() != null;
        subEntity.setCancelAtPeriodEnd(hasBooleanCancel || hasDateCancel);

        // Lógica de cambio de plan
        String currentStripePriceId = stripeSub.getItems().getData().get(0).getPrice().getId();
        if (!subEntity.getPlan().getStripePriceId().equals(currentStripePriceId)) {
            log.info("Cambio de plan detectado para suscripción {}. Nuevo Stripe Price ID: {}", stripeSub.getId(), currentStripePriceId);

            PlanEntity newPlan = planRepository.findByStripePriceId(currentStripePriceId)
                    .orElseThrow(() -> new WebhookSyncException("No se encontró el plan con stripe_price_id: " + currentStripePriceId));

            subEntity.setPlan(newPlan);

            UserEntity user = subEntity.getUser();
            user.setSubscriptionPlan(newPlan.getName());
            userRepository.save(user);
        }

        subscriptionRepository.save(subEntity);
        log.info("Suscripción {} actualizada en DB a status: {}", stripeSub.getId(), subEntity.getStatus());
    }


    private void applyTrialStatus(Subscription stripeSub, UserEntity user) {
        // Quitamos el try-catch genérico. Si la conversión de fecha falla, subirá como error de validación.
        if (stripeSub.getTrialEnd() != null) {
            LocalDateTime trialEnd = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(stripeSub.getTrialEnd()),
                    ZoneId.systemDefault()
            );
            user.setTrialEndDate(trialEnd);
            log.info("[TRIAL] Usuario {} en periodo de prueba hasta: {}", user.getId(), trialEnd);
        } else {
            user.setTrialEndDate(null);
            log.debug("[TRIAL] Suscripción estándar (Sin periodo de prueba) para usuario {}", user.getId());
        }
    }

    // =================================================================
    // MÉTODO PARA SUMAR LOS TOKENS A LA BASE DE DATOS
    // =================================================================
    private void handleExtraTokensPurchase(Map<String, String> metadata) {
        String userIdStr = metadata.get("user_id");
        String tokenAmountStr = metadata.get("token_amount");

        if (userIdStr == null || tokenAmountStr == null) {
//             log.error("❌ GRAVE: Faltan datos en la metadata para sumar los tokens. user_id={}, token_amount={}", userIdStr, tokenAmountStr);
            throw new InvalidStripePayloadException("Faltan datos en la metadata (user_id o token_amount) para sumar tokens.");        }

        Long userId = Long.parseLong(userIdStr);
        int tokensToAdd = Integer.parseInt(tokenAmountStr);

//         log.info("🔍 Buscando uso de plan para el usuario con ID {}...", userId);

        // 1. Buscamos al usuario para poder relacionarlo con el nuevo registro si es necesario
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));
        // 2. Buscamos el registro de uso del plan. Si NO existe, lo creamos nuevo.
        PlanUsageEntity usage = planUsageRepository.findByUserId(userId)
                .orElseGet(() -> {
//                     log.info("⚠️ No se encontró registro previo de PlanUsage para el usuario {}. Creando uno nuevo...", userId);
                    PlanUsageEntity newUsage = new PlanUsageEntity();
                    newUsage.setUser(user); // Asignamos la relación con el usuario
                    newUsage.setQueriesUsedToday(0);
                    newUsage.setFilesUploadedToday(0);
                    newUsage.setExtraTokens(0);
                    // Setear otras propiedades por defecto si tu entidad las requiere
                    return newUsage;
                });

        // 3. Sumamos los tokens (manejando posibles nulos de registros antiguos)
        int currentTokens = usage.getExtraTokens() != null ? usage.getExtraTokens() : 0;
        usage.setExtraTokens(currentTokens + tokensToAdd);

        // 4. Guardamos (hará UPDATE si existía, o INSERT si es nuevo)
        planUsageRepository.save(usage);

//         log.info("🎉 ¡ÉXITO! Se sumaron {} tokens al usuario {}. Total actual extra_tokens: {}", tokensToAdd, userId, usage.getExtraTokens());
    }
}


