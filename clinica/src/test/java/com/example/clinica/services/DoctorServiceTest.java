package com.example.clinica.services;

import com.example.clinica.dtos.DoctorUpdateDto;
import com.example.clinica.entities.DoctorEntity;
import com.example.clinica.exceptions.BusinessValidationException;
import com.example.clinica.exceptions.ResourceConflictException;
import com.example.clinica.exceptions.ResourceNotFoundException;
import com.example.clinica.models.AppointmentStatus;
import com.example.clinica.models.Doctor;
import com.example.clinica.repositories.AppointmentRepository;
import com.example.clinica.repositories.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private DoctorService doctorService;

    private DoctorEntity doctorEntity;
    private Doctor doctorModel;

    @BeforeEach
    void setUp() {
        doctorEntity = DoctorEntity.builder()
                .id(UUID.randomUUID())
                .license("MED-100")
                .fullName("Dr. Gregory House")
                .specialty("Diagnóstico")
                .consultationFee(100000.0)
                .build();

        doctorModel = Doctor.builder()
                .id(doctorEntity.getId())
                .license(doctorEntity.getLicense())
                .fullName(doctorEntity.getFullName())
                .specialty(doctorEntity.getSpecialty())
                .consultationFee(doctorEntity.getConsultationFee())
                .build();
    }

    @Test
    @DisplayName("Debe listar todos los médicos")
    void shouldFindAllDoctors() {
        when(doctorRepository.findAll()).thenReturn(List.of(doctorEntity));

        List<Doctor> result = doctorService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLicense()).isEqualTo("MED-100");
        verify(doctorRepository).findAll();
    }

    @Test
    @DisplayName("Debe consultar médico por licencia existente")
    void shouldFindDoctorByLicense() {
        when(doctorRepository.findByLicense("MED-100")).thenReturn(Optional.of(doctorEntity));

        Doctor result = doctorService.findByLicense("MED-100");

        assertThat(result).isNotNull();
        assertThat(result.getLicense()).isEqualTo("MED-100");
        assertThat(result.getFullName()).isEqualTo("Dr. Gregory House");
    }

    @Test
    @DisplayName("Debe lanzar 404 al consultar médico inexistente")
    void shouldThrowNotFoundWhenDoctorDoesNotExist() {
        when(doctorRepository.findByLicense("MED-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorService.findByLicense("MED-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("MED-999");
    }

    @Test
    @DisplayName("Debe crear un médico exitosamente")
    void shouldCreateDoctor() {
        when(doctorRepository.existsByLicense("MED-100")).thenReturn(false);
        when(doctorRepository.save(any(DoctorEntity.class))).thenReturn(doctorEntity);

        Doctor created = doctorService.create(doctorModel);

        assertThat(created).isNotNull();
        assertThat(created.getLicense()).isEqualTo("MED-100");
        verify(doctorRepository).save(any(DoctorEntity.class));
    }

    @Test
    @DisplayName("Debe lanzar 409 al crear médico con licencia duplicada")
    void shouldThrowConflictWhenDoctorLicenseAlreadyExists() {
        when(doctorRepository.existsByLicense("MED-100")).thenReturn(true);

        assertThatThrownBy(() -> doctorService.create(doctorModel))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("MED-100");

        verify(doctorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe actualizar médico existente")
    void shouldUpdateDoctor() {
        DoctorUpdateDto updateDto = DoctorUpdateDto.builder()
                .fullName("Dr. House Modificado")
                .specialty("Infectología")
                .consultationFee(120000.0)
                .build();

        when(doctorRepository.findByLicense("MED-100")).thenReturn(Optional.of(doctorEntity));
        when(doctorRepository.save(any(DoctorEntity.class))).thenReturn(doctorEntity);

        Doctor updated = doctorService.update("MED-100", updateDto);

        assertThat(updated).isNotNull();
        verify(doctorRepository).save(doctorEntity);
    }

    @Test
    @DisplayName("Debe eliminar médico sin citas SCHEDULED")
    void shouldDeleteDoctorWithoutScheduledAppointments() {
        when(doctorRepository.findByLicense("MED-100")).thenReturn(Optional.of(doctorEntity));
        when(appointmentRepository.existsByDoctorLicenseAndStatus("MED-100", AppointmentStatus.SCHEDULED)).thenReturn(false);

        doctorService.delete("MED-100");

        verify(doctorRepository).delete(doctorEntity);
    }

    @Test
    @DisplayName("Debe rechazar con 422 eliminar médico con citas SCHEDULED")
    void shouldThrowUnprocessableEntityWhenDeletingDoctorWithScheduledAppointments() {
        when(doctorRepository.findByLicense("MED-100")).thenReturn(Optional.of(doctorEntity));
        when(appointmentRepository.existsByDoctorLicenseAndStatus("MED-100", AppointmentStatus.SCHEDULED)).thenReturn(true);

        assertThatThrownBy(() -> doctorService.delete("MED-100"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("citas programadas");

        verify(doctorRepository, never()).delete(any());
    }
}
