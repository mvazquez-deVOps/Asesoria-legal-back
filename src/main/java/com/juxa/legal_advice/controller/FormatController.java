package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.service.AiBucketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/formats")
@RequiredArgsConstructor
public class FormatController {

    private final AiBucketService bucketService;

    // Endpoint para que Paws muestre la lista de plantillas al abogado
    @GetMapping("/list")
    public ResponseEntity<List<String>> getAvailableFormats() {
        return ResponseEntity.ok(bucketService.listAvailableFormats());
    }

    // Endpoint para limpiar caché tras subir un nuevo archivo al bucket
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshCache() {
        bucketService.clearCache();
        return ResponseEntity.ok(Map.of("message", "Caché de formatos y documentos reiniciada exitosamente."));
    }
}
