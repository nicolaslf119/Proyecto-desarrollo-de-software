package com.example.clinica.mappers;

import com.example.clinica.dtos.PatientCreateDto;
import com.example.clinica.dtos.PatientResponseDto;
import com.example.clinica.dtos.PatientUpdateDto;
import com.example.clinica.entities.PatientEntity;
import com.example.clinica.models.Patient;

public class PatientMapper {

    private PatientMapper() {
        // Constructor privado para clase utilitaria
    }

    public static Patient toModel(PatientCreateDto dto) {
        if (dto == null) {
            return null;
        }
        return Patient.builder()
                .document(dto.getDocument())
                .fullName(dto.getFullName())
                .birthDate(dto.getBirthDate())
                .insuranceType(dto.getInsuranceType())
                .noShowCount(0)
                .build();
    }

    public static Patient toModel(PatientEntity entity) {
        if (entity == null) {
            return null;
        }
        return Patient.builder()
                .id(entity.getId())
                .document(entity.getDocument())
                .fullName(entity.getFullName())
                .birthDate(entity.getBirthDate())
                .insuranceType(entity.getInsuranceType())
                .noShowCount(entity.getNoShowCount())
                .build();
    }

    public static PatientEntity toEntity(Patient model) {
        if (model == null) {
            return null;
        }
        return PatientEntity.builder()
                .id(model.getId())
                .document(model.getDocument())
                .fullName(model.getFullName())
                .birthDate(model.getBirthDate())
                .insuranceType(model.getInsuranceType())
                .noShowCount(model.getNoShowCount())
                .build();
    }

    public static PatientResponseDto toResponseDto(Patient model) {
        if (model == null) {
            return null;
        }
        return PatientResponseDto.builder()
                .id(model.getId())
                .document(model.getDocument())
                .fullName(model.getFullName())
                .birthDate(model.getBirthDate())
                .insuranceType(model.getInsuranceType())
                .noShowCount(model.getNoShowCount())
                .build();
    }

    public static void updateEntity(PatientEntity entity, PatientUpdateDto dto) {
        if (entity != null && dto != null) {
            entity.setFullName(dto.getFullName());
            entity.setBirthDate(dto.getBirthDate());
            entity.setInsuranceType(dto.getInsuranceType());
        }
    }
}
