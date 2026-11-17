package com.example.clinica.services;

import com.example.clinica.dtos.PatientUpdateDto;
import com.example.clinica.entities.PatientEntity;
import com.example.clinica.exceptions.BusinessValidationException;
import com.example.clinica.exceptions.ResourceConflictException;
import com.example.clinica.exceptions.ResourceNotFoundException;
import com.example.clinica.mappers.PatientMapper;
import com.example.clinica.models.AppointmentStatus;
import com.example.clinica.models.Patient;
import com.example.clinica.repositories.AppointmentRepository;
import com.example.clinica.repositories.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public List<Patient> findAll() {
        return patientRepository.findAll().stream()
                .map(PatientMapper::toModel)
                .toList();
    }

    @Transactional(readOnly = true)
    public Patient findByDocument(String document) {
        return patientRepository.findByDocument(document)
                .map(PatientMapper::toModel)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente con documento " + document + " no encontrado"));
    }

    @Transactional
    public Patient create(Patient patient) {
        if (patientRepository.existsByDocument(patient.getDocument())) {
            throw new ResourceConflictException("Ya existe un paciente con el documento " + patient.getDocument());
        }
        PatientEntity entity = PatientMapper.toEntity(patient);
        PatientEntity saved = patientRepository.save(entity);
        return PatientMapper.toModel(saved);
    }

    @Transactional
    public Patient update(String document, PatientUpdateDto dto) {
        PatientEntity entity = patientRepository.findByDocument(document)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente con documento " + document + " no encontrado"));

        PatientMapper.updateEntity(entity, dto);
        PatientEntity updated = patientRepository.save(entity);
        return PatientMapper.toModel(updated);
    }

    @Transactional
    public void delete(String document) {
        PatientEntity entity = patientRepository.findByDocument(document)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente con documento " + document + " no encontrado"));

        boolean hasScheduledAppointments = appointmentRepository.existsByPatientDocumentAndStatus(document, AppointmentStatus.SCHEDULED);
        if (hasScheduledAppointments) {
            throw new BusinessValidationException("No se puede eliminar el paciente con documento " + document + " porque tiene citas programadas (SCHEDULED)");
        }

        patientRepository.delete(entity);
    }
}
