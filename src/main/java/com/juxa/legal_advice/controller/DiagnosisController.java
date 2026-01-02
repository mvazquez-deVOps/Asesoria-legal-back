package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.dto.DiagnosisDTO;
import com.juxa.legal_advice.model.DiagnosisResponse;
import com.juxa.legal_advice.service.DiagnosisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/diagnoses")
@RequiredArgsConstructor // Esto genera el constructor automáticamente para diagnosisService
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    @PostMapping
    public ResponseEntity<DiagnosisResponse> save(@RequestBody DiagnosisDTO diagnosis) {
        return ResponseEntity.ok(diagnosisService.save(diagnosis));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DiagnosisDTO>> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(diagnosisService.findByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiagnosisDTO> getById(@PathVariable String id) {
        DiagnosisDTO diagnosis = diagnosisService.findById(id);
        return diagnosis != null ? ResponseEntity.ok(diagnosis) : ResponseEntity.notFound().build();
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<List<DiagnosisDTO>> getByUserEmail(@PathVariable String email) {
        return ResponseEntity.ok(diagnosisService.findByUserEmail(email));
    }
}