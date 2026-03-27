package com.juxa.legal_advice.service.payment;

import com.juxa.legal_advice.model.*;
import com.juxa.legal_advice.repository.*;
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
import java.util.Map;
import java.util.Optional;

@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @Autowired
    private ProcessedStripeEventRepository processedStripeEventRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlanRepository planRepository;
    @Autowired // <-- Faltaba inyectar esto
    private PlanUsageRepository planUsageRepository;


    public String handleWebhook(String payload, String sigHeader) {
        Event event;



        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            log.info("🔔 Webhook recibido exitosamente: Tipo de evento = {}", event.getType());
        } catch (SignatureVerificationException e) {
            log.error("❌ Error de firma en Webhook: Alguien intentó mandar una petición falsa");
            throw new RuntimeException("Firma de Stripe inválida");
        }

        String eventId = event.getId();
        if (processedStripeEventRepository.existsById(eventId)) {
            log.info("♻️ [IDEMPOTENCIA] El evento {} ({}) ya fue procesado anteriormente. Ignorando.", eventId, event.getType());
            return "Evento duplicado ignorado exitosamente"; // Devolvemos texto para que el Controller mande HTTP 200 OK a Stripe
        }

        // Si es la primera vez que vemos este evento, lo registramos inmediatamente
        ProcessedStripeEventEntity processedEvent = ProcessedStripeEventEntity.builder()
                .id(eventId)
                .build();
        processedStripeEventRepository.save(processedEvent);
        log.info("🔒 Evento {} registrado en la base de datos para evitar futuros duplicados.", eventId);

        StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);
        if (stripeObject == null) {
            log.warn("⚠️ No se pudo deserializar el objeto Stripe del evento.");
            return "Objeto nulo ignorado";
        }

        try {
            switch (event.getType()) {
                case "checkout.session.completed":
                    log.info("🚀 Procesando checkout.session.completed...");
                    Session session = (Session) stripeObject;

                    Map<String, String> metadata = session.getMetadata();

                    // 👇 CAMBIO CLAVE: Aceptar "paid" o "no_payment_required"
                    String paymentStatus = session.getPaymentStatus();
                    if ("paid".equals(paymentStatus) || "no_payment_required".equals(paymentStatus)) {

                        if (metadata != null && "extra_tokens".equals(metadata.get("payment_type"))) {
                            log.info("🪙 Detectada compra de tokens extra. Procesando...");
                            handleExtraTokensPurchase(metadata);
                            return "Compra de tokens procesada exitosamente";
                        } else {
                            log.info("📅 Detectada compra de suscripción/trial. Procesando...");
                            handleCheckoutSessionCompleted(session);
                            return "Suscripción inicial guardada";
                        }
                    } else {
                        log.warn("⚠️ Checkout completado pero el pago no es válido. Status: {}", paymentStatus);
                        return "Checkout completado sin pago válido";
                    }
                case "invoice.paid":
                    log.info("💰 Procesando invoice.paid...");
                    handleInvoicePayment((Invoice) stripeObject, true);
                    return "Pago exitoso registrado";

                case "invoice.payment_failed":
                    log.error("🚫 Procesando invoice.payment_failed...");
                    handleInvoicePayment((Invoice) stripeObject, false);
                    return "Fallo de pago registrado";

                case "customer.subscription.deleted":
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

        applyTrialStatus(stripeSub, user);
        userRepository.save(user);


        // 2. Revisamos si ya existe una suscripción
        Optional<SubscriptionEntity> existingSub = subscriptionRepository.findByUserId(userId);
        SubscriptionEntity subToSave;

        LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeSub.getStartDate()), ZoneId.systemDefault());
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
        log.info("🚨 [PASO 1] INICIANDO PROCESAMIENTO DE FACTURA 🚨");
        log.info("👉 ID de Factura: {}", invoice.getId());

        String subscriptionId = null;

        // Intentamos extraer el subscription ID de las líneas de la factura
        //Si no encontramos la metadata en la session entonces lo buscamos en la subscription
        if (invoice.getLines() != null && invoice.getLines().getData() != null && !invoice.getLines().getData().isEmpty()) {
            subscriptionId = invoice.getLines().getData().get(0).getSubscription();
            log.info("👉 [PASO 2] Buscando ID en Line Items. Resultado: {}", subscriptionId);

            if (subscriptionId == null && invoice.getLines().getData().get(0).getParent() != null &&
                    invoice.getLines().getData().get(0).getParent().getSubscriptionItemDetails() != null) {
                subscriptionId = invoice.getLines().getData().get(0).getParent().getSubscriptionItemDetails().getSubscription();
                log.info("👉 [PASO 2.1] Buscando ID en Parent details. Resultado: {}", subscriptionId);
            }
        } else {
            log.warn("⚠️ [PASO 2] La factura no tiene Line Items (getLines() es null o vacío)");
        }

        if (subscriptionId == null) {
            log.warn("🛑 [PASO 4 - ABORTO] Factura ignorada porque no se encontró ID de suscripción por ningún lado.");
            log.info("=========================================================");
            return;
        }

        if (paymentHistoryRepository.existsByStripeInvoiceId(invoice.getId())) {
            log.info("♻️ [IDEMPOTENCIA] La factura {} ya fue procesada anteriormente. Ignorando evento duplicado.", invoice.getId());
            return; // Salimos sin lanzar error para que Stripe reciba su 200 OK
        }

        log.info("🔍 [PASO 5] Buscando suscripción {} en la Base de Datos...", subscriptionId);

        final String finalSubscriptionId = subscriptionId;

        subscriptionRepository.findByStripeSubscriptionId(finalSubscriptionId)
                .ifPresentOrElse(subEntity -> {
                    log.info("✅ [PASO 6 - ÉXITO] Suscripción encontrada en BD (ID Interno: {}).", subEntity.getId());

                    //  Solo hacemos esto para que los datos ingresen correctamente a la base de datos
                    BigDecimal amount = BigDecimal.valueOf(invoice.getAmountPaid()).divide(BigDecimal.valueOf(100));

                    if (!isSuccess && amount.compareTo(BigDecimal.ZERO) == 0) {
                        amount = BigDecimal.valueOf(invoice.getAmountDue()).divide(BigDecimal.valueOf(100));
                    }

                    String finalStatus = invoice.getStatus() != null ? invoice.getStatus() : (isSuccess ? "paid" : "failed");

                    // 2. Imprimir los datos exactos que vamos a meter a la BD
                    log.info("👉 [PASO 6.1] VERIFICACIÓN DE DATOS ANTES DE GUARDAR:");
                    log.info("   - subscription_id (FK): {}", subEntity.getId());
                    log.info("   - stripe_invoice_id: {}", invoice.getId());
                    log.info("   - amount_paid: {}", amount);
                    log.info("   - status: {}", finalStatus);

                    // 3. Construir entidad
                    PaymentHistoryEntity payment = PaymentHistoryEntity.builder()
                            .subscription(subEntity)
                            .stripeInvoiceId(invoice.getId())
                            .amountPaid(amount)
                            .status(finalStatus)
                            .build();

                    // 4. Guardar y FORZAR el insert inmediato en la BD
                    log.info("👉 [PASO 6.2] Ejecutando saveAndFlush()...");
                    payment = paymentHistoryRepository.saveAndFlush(payment);

                    log.info("🎉 [FINAL] ¡PaymentHistory insertado correctamente! ID generado en la BD: {}", payment.getId());
                    log.info("=========================================================");

                }, () -> {
                    log.error("❌ [PASO 6 - ERROR] La suscripción {} AÚN NO EXISTE en DB.", finalSubscriptionId);
                    log.error("💡 EXPLICACIÓN: Stripe mandó 'invoice.paid' ANTES de que se guardara la suscripción.");
                    log.info("=========================================================");
                    throw new RuntimeException("Suscripción no encontrada en DB. Forzando reintento del webhook de pago.");
                });
    }

    private void handleSubscriptionUpdate(Subscription eventSub) {
        log.info("🔄 Actualizando status de la suscripción {}...", eventSub.getId());

        try {
            Subscription stripeSub = Subscription.retrieve(eventSub.getId());

            log.info("👉 Status fresco extraído: {}", stripeSub.getStatus());
            log.info("👉 CancelAtPeriodEnd fresco extraído: {}", stripeSub.getCancelAtPeriodEnd());
            log.info("👉 CancelAt (fecha) fresco extraído: {}", stripeSub.getCancelAt());

            subscriptionRepository.findByStripeSubscriptionId(stripeSub.getId())
                    .ifPresentOrElse(subEntity -> {

                        // 1. TU LÓGICA ORIGINAL DE ESTATUS Y FECHAS (INTACTA)
                        subEntity.setStatus(stripeSub.getStatus());

                        if ("canceled".equals(stripeSub.getStatus()) || "unpaid".equals(stripeSub.getStatus())) {
                            subEntity.setCurrentPeriodEnd(LocalDateTime.now());
                            log.info("🛑 Suscripción cancelada. Fecha de expiración forzada a hoy.");
                        } else {
                            Long periodEndEpoch = stripeSub.getItems().getData().get(0).getCurrentPeriodEnd();
                            LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochSecond(periodEndEpoch), ZoneId.systemDefault());
                            subEntity.setCurrentPeriodEnd(end);
                        }

                        // 2. TU LÓGICA ORIGINAL DE CANCELACIÓN (INTACTA)
                        boolean hasBooleanCancel = stripeSub.getCancelAtPeriodEnd() != null && stripeSub.getCancelAtPeriodEnd();
                        boolean hasDateCancel = stripeSub.getCancelAt() != null;
                        boolean isScheduledToCancel = hasBooleanCancel || hasDateCancel;

                        subEntity.setCancelAtPeriodEnd(isScheduledToCancel);

                        // ====================================================================
                        // 3. 👇 AQUÍ INSERTAMOS SOLAMENTE LA LÓGICA DE CAMBIO DE PLAN 👇
                        // ====================================================================
                        try {
                            // Extraemos el Price ID actual que Stripe está cobrando
                            String currentStripePriceId = stripeSub.getItems().getData().get(0).getPrice().getId();

                            // Si el Price ID de Stripe es diferente al que tenemos guardado, ¡hubo un cambio!
                            if (!subEntity.getPlan().getStripePriceId().equals(currentStripePriceId)) {
                                log.info("⬆️⬇️ ¡Cambio de plan detectado! Nuevo Stripe Price ID: {}", currentStripePriceId);

                                // Buscamos el nuevo plan en nuestra DB
                                PlanEntity newPlan = planRepository.findByStripePriceId(currentStripePriceId)
                                        .orElseThrow(() -> new RuntimeException("No se encontró el plan con stripe_price_id: " + currentStripePriceId));

                                // Actualizamos la suscripción con el nuevo plan
                                subEntity.setPlan(newPlan);

                                // Actualizamos también la tabla del usuario
                                UserEntity user = subEntity.getUser();
                                user.setSubscriptionPlan(newPlan.getName());
                                userRepository.save(user);

                                log.info("✅ Plan actualizado con éxito en BD al plan: {}", newPlan.getName());
                            }
                        } catch (Exception e) {
                            log.error("⚠️ Error intentando actualizar el plan de la suscripción: {}", e.getMessage());
                        }
                        // ====================================================================
                        // 👆 FIN DE LA LÓGICA DE CAMBIO DE PLAN 👆
                        // ====================================================================

                        // 4. GUARDAMOS LOS CAMBIOS FINALES
                        subscriptionRepository.save(subEntity);
                        log.info("✅ Suscripción actualizada en DB a status: {} y cancelAtPeriodEnd: {}",
                                subEntity.getStatus(), subEntity.isCancelAtPeriodEnd());

                    }, () -> log.warn("⚠️ Intentamos actualizar la suscripción {}, pero no existe en DB.", stripeSub.getId()));

        } catch (Exception e) {
            log.error("❌ Error al recuperar la suscripción actualizada de Stripe: {}", e.getMessage());
            throw new RuntimeException("Error al actualizar la suscripción", e);
        }
    }


    private void applyTrialStatus(Subscription stripeSub, UserEntity user) {
        try {
            // Si Stripe nos indica que hay una fecha de fin de Trial
            if (stripeSub.getTrialEnd() != null) {
                LocalDateTime trialEnd = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(stripeSub.getTrialEnd()),
                        ZoneId.systemDefault()
                );
                user.setTrialEndDate(trialEnd); // Asignamos la fecha a MySQL
                log.info("⏳ [TRIAL] Usuario {} en periodo de prueba hasta: {}", user.getEmail(), trialEnd);
            } else {
                // Si no hay trial, asegurarnos de que la columna sea null
                user.setTrialEndDate(null);
                log.info("▶️ [TRIAL] Suscripción estándar (Sin periodo de prueba) para {}", user.getEmail());
            }
        } catch (Exception e) {
            log.error("⚠️ Error aislado procesando la fecha de Trial: {}", e.getMessage());
        }
    }
    // ====================================================================

    // =================================================================
    // MÉTODO PARA SUMAR LOS TOKENS A LA BASE DE DATOS
    // =================================================================
    private void handleExtraTokensPurchase(Map<String, String> metadata) {
        String userIdStr = metadata.get("user_id");
        String tokenAmountStr = metadata.get("token_amount");

        if (userIdStr == null || tokenAmountStr == null) {
            log.error("❌ GRAVE: Faltan datos en la metadata para sumar los tokens. user_id={}, token_amount={}", userIdStr, tokenAmountStr);
            throw new RuntimeException("Metadata inválida para compra de tokens");
        }

        Long userId = Long.parseLong(userIdStr);
        int tokensToAdd = Integer.parseInt(tokenAmountStr);

        log.info("🔍 Buscando uso de plan para el usuario con ID {}...", userId);

        // 1. Buscamos al usuario para poder relacionarlo con el nuevo registro si es necesario
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + userId));

        // 2. Buscamos el registro de uso del plan. Si NO existe, lo creamos nuevo.
        PlanUsageEntity usage = planUsageRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("⚠️ No se encontró registro previo de PlanUsage para el usuario {}. Creando uno nuevo...", userId);
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

        log.info("🎉 ¡ÉXITO! Se sumaron {} tokens al usuario {}. Total actual extra_tokens: {}", tokensToAdd, userId, usage.getExtraTokens());
    }
}


