package com.example.clinica.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Doctor {
    private UUID id;
    private String license;
    private String fullName;
    private String specialty;
    private Double consultationFee;
}
