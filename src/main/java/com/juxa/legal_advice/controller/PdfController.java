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
        // 1. Obtenemos la entidad completa desde el servicio (una sola consulta)
        DiagnosisEntity entity = diagnosisService.findEntityById(id);

        // 2. Generamos la respuesta técnica (IA) basada en esa entidad
        DiagnosisResponse response = diagnosisService.generateResponse(entity);

        // 3. Obtenemos el plan con un valor por defecto seguro
        SubscriptionPlan plan = (entity.getPlan() != null) ? entity.getPlan() : SubscriptionPlan.SINGLE_DIAGNOSIS;

        // 4. Generamos el PDF
        byte[] pdfBytes = pdfService.generateDiagnosisPdf(response, plan);

        // 5. Retornamos el archivo
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dictamen-juxa-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}