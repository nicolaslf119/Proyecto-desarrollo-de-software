package com.example.clinica.services;

import com.example.clinica.dtos.DoctorUpdateDto;
import com.example.clinica.entities.DoctorEntity;
import com.example.clinica.exceptions.BusinessValidationException;
import com.example.clinica.exceptions.ResourceConflictException;
import com.example.clinica.exceptions.ResourceNotFoundException;
import com.example.clinica.mappers.DoctorMapper;
import com.example.clinica.models.AppointmentStatus;
import com.example.clinica.models.Doctor;
import com.example.clinica.repositories.AppointmentRepository;
import com.example.clinica.repositories.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public List<Doctor> findAll() {
        return doctorRepository.findAll().stream()
                .map(DoctorMapper::toModel)
                .toList();
    }

    @Transactional(readOnly = true)
    public Doctor findByLicense(String license) {
        return doctorRepository.findByLicense(license)
                .map(DoctorMapper::toModel)
                .orElseThrow(() -> new ResourceNotFoundException("Médico con licencia " + license + " no encontrado"));
    }

    @Transactional
    public Doctor create(Doctor doctor) {
        if (doctorRepository.existsByLicense(doctor.getLicense())) {
            throw new ResourceConflictException("Ya existe un médico con la licencia " + doctor.getLicense());
        }
        DoctorEntity entity = DoctorMapper.toEntity(doctor);
        DoctorEntity saved = doctorRepository.save(entity);
        return DoctorMapper.toModel(saved);
    }

    @Transactional
    public Doctor update(String license, DoctorUpdateDto dto) {
        DoctorEntity entity = doctorRepository.findByLicense(license)
                .orElseThrow(() -> new ResourceNotFoundException("Médico con licencia " + license + " no encontrado"));

        DoctorMapper.updateEntity(entity, dto);
        DoctorEntity updated = doctorRepository.save(entity);
        return DoctorMapper.toModel(updated);
    }

    @Transactional
    public void delete(String license) {
        DoctorEntity entity = doctorRepository.findByLicense(license)
                .orElseThrow(() -> new ResourceNotFoundException("Médico con licencia " + license + " no encontrado"));

        boolean hasScheduledAppointments = appointmentRepository.existsByDoctorLicenseAndStatus(license, AppointmentStatus.SCHEDULED);
        if (hasScheduledAppointments) {
            throw new BusinessValidationException("No se puede eliminar el médico con licencia " + license + " porque tiene citas programadas (SCHEDULED)");
        }

        doctorRepository.delete(entity);
    }
}
