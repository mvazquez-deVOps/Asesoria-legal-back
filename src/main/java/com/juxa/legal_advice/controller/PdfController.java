package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.model.DiagnosisEntity;
import com.juxa.legal_advice.model.DiagnosisResponse;
import com.juxa.legal_advice.model.SubscriptionPlan;
import com.juxa.legal_advice.service.DiagnosisService;
import com.juxa.legal_advice.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor // Inyecta los servicios automáticamente (final)
public class PdfController {

    private final DiagnosisService diagnosisService;
    private final PdfService pdfService;

    /**
     * Genera y descarga el PDF de un diagnóstico específico.
     * Versión 1.0.1: Optimización de consultas y manejo de planes por defecto.
     */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getDiagnosisPdf(@PathVariable Long id) {
        try {
            // 1. Buscamos la entidad completa (Datos de DB)
            DiagnosisEntity entity = diagnosisService.findEntityById(id);

            // 2. Generamos la respuesta técnica (IA) usando la entidad recuperada
            DiagnosisResponse response = diagnosisService.generateResponse(entity);

            // 3. Validamos el plan: si es nulo en DB, asignamos el plan base
            SubscriptionPlan plan = (entity.getPlan() != null)
                    ? entity.getPlan()
                    : SubscriptionPlan.SINGLE_DIAGNOSIS;

            // 4. Solicitamos al servicio la creación de los bytes del PDF
            byte[] pdfBytes = pdfService.generateDiagnosisPdf(response, plan);

            // 5. Configuramos las cabeceras para que el navegador lo trate como descarga
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename("Dictamen_Legal_JUXA_" + id + ".pdf")
                            .build()
            );

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            // Si el ID no existe o hay error, devolvemos un 404 o 500
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}