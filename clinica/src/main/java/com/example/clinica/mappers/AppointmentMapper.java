package com.example.clinica.mappers;

import com.example.clinica.dtos.AppointmentResponseDto;
import com.example.clinica.entities.AppointmentEntity;
import com.example.clinica.models.Appointment;

public class AppointmentMapper {

    private AppointmentMapper() {
        // Constructor privado para clase utilitaria
    }

    public static Appointment toModel(AppointmentEntity entity) {
        if (entity == null) {
            return null;
        }
        return Appointment.builder()
                .id(entity.getId())
                .number(entity.getNumber())
                .doctor(DoctorMapper.toModel(entity.getDoctor()))
                .patient(PatientMapper.toModel(entity.getPatient()))
                .dateTime(entity.getDateTime())
                .status(entity.getStatus())
                .amountToPay(entity.getAmountToPay())
                .build();
    }

    public static AppointmentEntity toEntity(Appointment model) {
        if (model == null) {
            return null;
        }
        return AppointmentEntity.builder()
                .id(model.getId())
                .number(model.getNumber())
                .doctor(DoctorMapper.toEntity(model.getDoctor()))
                .patient(PatientMapper.toEntity(model.getPatient()))
                .dateTime(model.getDateTime())
                .status(model.getStatus())
                .amountToPay(model.getAmountToPay())
                .build();
    }

    public static AppointmentResponseDto toResponseDto(Appointment model) {
        if (model == null) {
            return null;
        }
        return AppointmentResponseDto.builder()
                .id(model.getId())
                .number(model.getNumber())
                .doctor(DoctorMapper.toResponseDto(model.getDoctor()))
                .patient(PatientMapper.toResponseDto(model.getPatient()))
                .dateTime(model.getDateTime())
                .status(model.getStatus())
                .amountToPay(model.getAmountToPay())
                .build();
    }
}
