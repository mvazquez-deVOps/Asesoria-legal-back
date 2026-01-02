package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.model.DiagnosisResponse;
import com.juxa.legal_advice.model.DiagnosisEntity;
import com.juxa.legal_advice.model.SubscriptionPlan;
import com.juxa.legal_advice.repository.DiagnosisRepository;
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
    private final DiagnosisRepository diagnosisRepository; // Agregado para buscar el plan

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getDiagnosisPdf(@PathVariable Long id) {
        DiagnosisResponse response = diagnosisService.findResponseById(id);
        DiagnosisEntity entity = diagnosisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el diagnóstico con ID: " + id));

        SubscriptionPlan planParaPdf = entity.getPlan();
        if (planParaPdf == null) {
            planParaPdf = SubscriptionPlan.SINGLE_DIAGNOSIS;

        }

        // AMBOS parámetros al PdfService
        byte[] pdfBytes = pdfService.generateDiagnosisPdf(response, planParaPdf);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=diagnosis-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}



