package com.example.clinica.models;

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
public class Appointment {
    private UUID id;
    private String number;
    private Doctor doctor;
    private Patient patient;
    private LocalDateTime dateTime;
    private AppointmentStatus status;
    private Double amountToPay;
}
