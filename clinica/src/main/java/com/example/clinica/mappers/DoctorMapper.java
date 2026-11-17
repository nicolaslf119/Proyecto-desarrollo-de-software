package com.example.clinica.mappers;

import com.example.clinica.dtos.DoctorCreateDto;
import com.example.clinica.dtos.DoctorResponseDto;
import com.example.clinica.dtos.DoctorUpdateDto;
import com.example.clinica.entities.DoctorEntity;
import com.example.clinica.models.Doctor;

public class DoctorMapper {

    private DoctorMapper() {
        // Constructor privado para clase utilitaria
    }

    public static Doctor toModel(DoctorCreateDto dto) {
        if (dto == null) {
            return null;
        }
        return Doctor.builder()
                .license(dto.getLicense())
                .fullName(dto.getFullName())
                .specialty(dto.getSpecialty())
                .consultationFee(dto.getConsultationFee())
                .build();
    }

    public static Doctor toModel(DoctorEntity entity) {
        if (entity == null) {
            return null;
        }
        return Doctor.builder()
                .id(entity.getId())
                .license(entity.getLicense())
                .fullName(entity.getFullName())
                .specialty(entity.getSpecialty())
                .consultationFee(entity.getConsultationFee())
                .build();
    }

    public static DoctorEntity toEntity(Doctor model) {
        if (model == null) {
            return null;
        }
        return DoctorEntity.builder()
                .id(model.getId())
                .license(model.getLicense())
                .fullName(model.getFullName())
                .specialty(model.getSpecialty())
                .consultationFee(model.getConsultationFee())
                .build();
    }

    public static DoctorResponseDto toResponseDto(Doctor model) {
        if (model == null) {
            return null;
        }
        return DoctorResponseDto.builder()
                .id(model.getId())
                .license(model.getLicense())
                .fullName(model.getFullName())
                .specialty(model.getSpecialty())
                .consultationFee(model.getConsultationFee())
                .build();
    }

    public static void updateEntity(DoctorEntity entity, DoctorUpdateDto dto) {
        if (entity != null && dto != null) {
            entity.setFullName(dto.getFullName());
            entity.setSpecialty(dto.getSpecialty());
            entity.setConsultationFee(dto.getConsultationFee());
        }
    }
}
