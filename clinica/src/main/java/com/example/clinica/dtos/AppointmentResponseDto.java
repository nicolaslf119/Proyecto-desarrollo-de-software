package com.example.clinica.dtos;

import com.example.clinica.models.AppointmentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponseDto {
    private UUID id;
    private String number;
    private DoctorResponseDto doctor;
    private PatientResponseDto patient;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateTime;

    private AppointmentStatus status;
    private Double amountToPay;
}
