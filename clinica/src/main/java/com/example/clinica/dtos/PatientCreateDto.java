package com.example.clinica.dtos;

import com.example.clinica.models.InsuranceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientCreateDto {

    @NotBlank(message = "El documento es obligatorio")
    private String document;

    @NotBlank(message = "El nombre completo es obligatorio")
    private String fullName;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    private LocalDate birthDate;

    @NotNull(message = "El tipo de seguro es obligatorio")
    private InsuranceType insuranceType;
}
