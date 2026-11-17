package com.example.clinica.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorUpdateDto {

    @NotBlank(message = "El nombre completo es obligatorio")
    private String fullName;

    @NotBlank(message = "La especialidad es obligatoria")
    private String specialty;

    @NotNull(message = "La tarifa de consulta es obligatoria")
    @Positive(message = "La tarifa de consulta debe ser mayor a cero")
    private Double consultationFee;
}
