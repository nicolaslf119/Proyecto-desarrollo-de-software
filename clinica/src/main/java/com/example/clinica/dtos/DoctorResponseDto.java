package com.example.clinica.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorResponseDto {
    private UUID id;
    private String license;
    private String fullName;
    private String specialty;
    private Double consultationFee;
}
