package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.dto.ReporteRequestDTO;
import com.juxa.legal_advice.service.UserService;
import com.juxa.legal_advice.model.ReporteEntity;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.ReporteRepository;
import com.juxa.legal_advice.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.opentelemetry.api.internal.ApiUsageLogger.log;

@RestController
@RequestMapping("/api/denuncias")
@RequiredArgsConstructor
public class DenunciaController {
    private static final Logger log = LoggerFactory.getLogger(DenunciaController.class);

    private final ReporteRepository reporteRepository;
    private final UserService userService;
    private final JavaMailSender mailSender;

    @PostMapping("/enviar")
    public ResponseEntity<?> recibirDenuncia(@Valid @RequestBody ReporteRequestDTO dto) {
        log.info("adas");
        try{
            UserEntity currentUser = userService.getCurrentAuthenticatedUser();
            log.info("adassdsadasd");
            ReporteEntity reporte = ReporteEntity.builder()
                    .usuario(currentUser)
                    .nivel(dto.getNivel())
                    .categoria(dto.getCategoria())
                    .nombreIncidencia(dto.getNombre())
                    .descripcion(dto.getDescripcion())
                    .email(currentUser.getEmail())
                    .plataforma(dto.getPlataforma())
                    .fechaHoraCliente(dto.getFechaHora())
                    .build();
            reporteRepository.save(reporte);

            enviarCorreoSoporteAsync(reporte);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Reporte procesado correctamente"));
        } catch (Exception e) {
            log.error("Error al guardar la incidencia", e);
            return ResponseEntity.internalServerError().body(Map.of("error","Error al procesar la solicitud"));
        }
    }

    @Async
    protected void enviarCorreoSoporteAsync(ReporteEntity reporte) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("notificaciones@juxa.mx");
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
                    reporte.getUsuario().getId(),
                    reporte.getEmail(),
                    reporte.getNivel(),
                    reporte.getCategoria(),
                    reporte.getPlataforma(),
                    reporte.getDescripcion()
            ));
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Error al enviar el correo a soporte para la incidencia ID {}", reporte.getId(), e);
        }
    }
}
