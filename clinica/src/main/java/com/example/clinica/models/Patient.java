package com.example.clinica.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
    private UUID id;
    private String document;
    private String fullName;
    private LocalDate birthDate;
    private InsuranceType insuranceType;
    private int noShowCount;
}
