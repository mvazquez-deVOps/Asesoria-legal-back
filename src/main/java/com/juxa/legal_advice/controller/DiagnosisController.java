package com.juxa.legal_advice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/diagnoses")
public class  DiagnosisController {
    @PostMapping
    public ResponseEntity<DiagnosisDTO> save(@RequestBody DiagnosisDTO diagnosis) {
        return ResponseEntity.ok(diagnosisService.save(diagnosis));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DiagnosisDTO>> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(diagnosisService.findByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiagnosisDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(diagnosisService.findById(id));
    }

}
