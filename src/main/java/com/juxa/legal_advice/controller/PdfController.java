package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.model.DiagnosisResponse;
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
        DiagnosisResponse response = diagnosisService.findResponseById(id);
        byte[] pdfBytes = pdfService.generateDiagnosisPdf(response);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=diagnosis-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
