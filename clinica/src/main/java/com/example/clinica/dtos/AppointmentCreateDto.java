package com.example.clinica.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentCreateDto {

    @NotBlank(message = "La licencia del médico es obligatoria")
    private String doctorLicense;

    @NotBlank(message = "El documento del paciente es obligatorio")
    private String patientDocument;

    @NotNull(message = "La fecha y hora de la cita son obligatorias")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateTime;
}
