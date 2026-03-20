package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.dto.PlanResponseDTO;
import com.juxa.legal_advice.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    // Puedes hacer que este endpoint sea público en tu SecurityConfig (.permitAll())
    // para que los usuarios no logueados puedan ver los precios.
    @GetMapping
    public ResponseEntity<List<PlanResponseDTO>> getAvailablePlans() {
        List<PlanResponseDTO> plans = planService.getAllPlans();
        return ResponseEntity.ok(plans);
    }
}