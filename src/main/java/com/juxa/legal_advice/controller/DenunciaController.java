package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.model.ReporteEntity;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.ReporteRepository;
import com.juxa.legal_advice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;

@RestController
@RequestMapping("/api/denuncias")
@RequiredArgsConstructor
public class DenunciaController {
    private final ReporteRepository reporteRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @PostMapping("/enviar")
    public ResponseEntity<?> recibirDenuncia(@RequestBody Map<String, String> data) {
        try{
            String userIdStr = String.valueOf(data.get("userId"));
            UserEntity user = (data.get("userId") != null) ?
                    userRepository.findById(Long.parseLong(userIdStr)).orElse(null) : null;

            ReporteEntity reporte = ReporteEntity.builder()
                    .nivel(data.get("nivel"))
                    .categoria(data.get("categoria"))
                    .nombreIncidencia(data.get("nombre"))
                    .descripcion(data.get("descripcion"))
                    .email(data.get("contactoEmail"))
                    .plataforma(data.get("plataforma"))
                    .fechaHoraCliente(data.get("fechaHora"))
                    .build();
            reporteRepository.save(reporte);
            enviarCorreoSoporte(reporte);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Reporte guardado en BD"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private void enviarCorreoSoporte(ReporteEntity reporte) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("soporte@juxa.mx");
        message.setSubject("NUEVA INCIDENCIA: " + reporte.getNombreIncidencia());
        message.setText(String.format(
                "Se ha recibido un nuevo reporte de error:\n\n" +
                        "Usuario ID: %s\n" +
                        "Email de contacto: %s\n" +
                        "Nivel: %s\n" +
                        "Categoría: %s\n" +
                        "Plataforma: %s\n" +
                        "Descripción: %s\n",
                reporte.getUsuario() != null ? reporte.getUsuario().getId() : "Invitado",
                reporte.getEmail(),
                reporte.getNivel(),
                reporte.getCategoria(),
                reporte.getPlataforma(),
                reporte.getDescripcion()
        ));
        mailSender.send(message);
    }
}
