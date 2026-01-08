package com.juxa.legal_advice.service;

import com.juxa.legal_advice.dto.DiagnosisDTO;
import com.juxa.legal_advice.model.DiagnosisEntity;
import com.juxa.legal_advice.model.DiagnosisResponse;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.DiagnosisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiagnosisService {

    private final DiagnosisRepository diagnosisRepository;
    private final GeminiService geminiService;

    // 1. Guardar diagnóstico normal
    public DiagnosisResponse save(DiagnosisDTO dto) {
        DiagnosisEntity entity = new DiagnosisEntity();
        if (dto.getUserData() != null) {
            entity.setUserId(dto.getUserData().getUserId());
            entity.setDescription(dto.getUserData().getDescription());
        }
        entity.setCreatedAt(LocalDateTime.now());
        entity.setPaid(true);
        return generateResponse(diagnosisRepository.save(entity));
    }

    // 2. Guardar interacción del chat (VERSIÓN CORREGIDA Y SINCRONIZADA)
    public void saveFromChat(Map<String, Object> payload, String aiResponse) {
        try {
            Map<String, Object> userDataMap = (Map<String, Object>) payload.get("userData");

            // 1. CORRECCIÓN: El Front ahora envía 'currentMessage'
            String userMsg = (String) payload.get("currentMessage");
            if (userMsg == null) userMsg = (String) payload.get("message"); // Backup por si acaso

            // 2. CORRECCIÓN: El Front envía 'id', el Back a veces espera 'userId'
            String userId = null;
            if (userDataMap != null) {
                userId = userDataMap.get("id") != null ?
                        userDataMap.get("id").toString() :
                        (String) userDataMap.get("userId");
            }

            DiagnosisEntity entity;
            if (userId != null && !userId.isEmpty()) {
                entity = diagnosisRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                        .orElse(new DiagnosisEntity());
            } else {
                entity = new DiagnosisEntity();
            }

            // Si es nuevo, inicializamos
            if (entity.getId() == null) {
                entity.setUserId(userId);
                entity.setCreatedAt(LocalDateTime.now());
                entity.setPaid(true); // Permitir que aparezca en el dashboard
            }

            // 3. CORRECCIÓN: Aseguramos que el historial no se pierda
            String historialPrevio = (entity.getHistory() != null) ? entity.getHistory() : "";
            String nuevaInteraccion = "Usuario: " + (userMsg != null ? userMsg : "Consulta") + "\n" +
                    "IA: " + aiResponse + "\n\n";

            entity.setHistory(historialPrevio + nuevaInteraccion);

            diagnosisRepository.save(entity);
            System.out.println(" Persistencia exitosa en Cloud SQL para usuario: " + userId);

        } catch (Exception e) {
            System.err.println(" Error crítico guardando chat: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // 3. Buscar por ID para el controlador
    public DiagnosisDTO findById(String id) {
        return diagnosisRepository.findById(Long.parseLong(id))
                .map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException("No encontrado"));
    }

    // 4. Buscar por Usuario para el controlador
    public List<DiagnosisDTO> findByUser(String userId) {
        return diagnosisRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private DiagnosisDTO mapToDTO(DiagnosisEntity entity) {
        DiagnosisDTO dto = new DiagnosisDTO();
        dto.setId(String.valueOf(entity.getId()));
        return dto;
    }

    public DiagnosisResponse generateResponse(DiagnosisEntity entity) {
        String summary = geminiService.generateLegalSummary(entity);
        return DiagnosisResponse.builder()
                .diagnosisId(String.valueOf(entity.getId()))
                .summary(summary)
                .steps(Arrays.asList("Consultar abogado experto", "Reunir evidencia"))
                .build();
    }

    public DiagnosisEntity findEntityById(Long id) {
        return diagnosisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el registro con ID: " + id));
    }
}