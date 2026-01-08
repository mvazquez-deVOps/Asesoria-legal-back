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
@RequiredArgsConstructor
public class PdfController {

    private final DiagnosisService diagnosisService;
    private final PdfService pdfService;

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getDiagnosisPdf(@PathVariable Long id) {
        try {
            // 1. Recuperamos la entidad de la base de datos
            DiagnosisEntity entity = diagnosisService.findEntityById(id);

            // 2. Generamos la respuesta técnica (IA) necesaria para el PDF
            DiagnosisResponse response = diagnosisService.generateResponse(entity);

            // 3. Obtenemos el plan (o asignamos el plan por defecto)
            SubscriptionPlan plan = (entity.getPlan() != null)
                    ? entity.getPlan()
                    : SubscriptionPlan.SINGLE_DIAGNOSIS;

            // 4. CORRECCIÓN: Pasamos los DOS argumentos requeridos por el servicio
            byte[] pdfBytes = pdfService.generateDiagnosisPdf(response, plan);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename("Dictamen_Legal_JUXA_" + id + ".pdf")
                            .build()
            );

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}