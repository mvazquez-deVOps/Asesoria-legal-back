package com.juxa.legal_advice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteRequestDTO {
    @NotBlank(message = "El nivel es obligatorio")
    private String nivel;

    @NotBlank(message = "La categoría es obligatoria")
    private String categoria;

    @NotBlank(message = "El nombre para la incidencia es obligatorio")
    @Size(max = 150, message = "El título es demasiado largo")
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 2000, message = "La descripción no puede exceder los 2000 caracteres")
    private String descripcion;

    private String plataforma;
    private String fechaHora;
}
