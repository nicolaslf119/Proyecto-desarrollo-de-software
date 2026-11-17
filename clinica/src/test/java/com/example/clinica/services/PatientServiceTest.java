package com.example.clinica.services;

import com.example.clinica.dtos.PatientUpdateDto;
import com.example.clinica.entities.PatientEntity;
import com.example.clinica.exceptions.BusinessValidationException;
import com.example.clinica.exceptions.ResourceConflictException;
import com.example.clinica.exceptions.ResourceNotFoundException;
import com.example.clinica.models.AppointmentStatus;
import com.example.clinica.models.InsuranceType;
import com.example.clinica.models.Patient;
import com.example.clinica.repositories.AppointmentRepository;
import com.example.clinica.repositories.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private PatientService patientService;

    private PatientEntity patientEntity;
    private Patient patientModel;

    @BeforeEach
    void setUp() {
        patientEntity = PatientEntity.builder()
                .id(UUID.randomUUID())
                .document("1020304050")
                .fullName("Juan Perez")
                .birthDate(LocalDate.of(1995, 5, 20))
                .insuranceType(InsuranceType.BASIC)
                .noShowCount(0)
                .build();

        patientModel = Patient.builder()
                .id(patientEntity.getId())
                .document(patientEntity.getDocument())
                .fullName(patientEntity.getFullName())
                .birthDate(patientEntity.getBirthDate())
                .insuranceType(patientEntity.getInsuranceType())
                .noShowCount(0)
                .build();
    }

    @Test
    @DisplayName("Debe listar todos los pacientes")
    void shouldFindAllPatients() {
        when(patientRepository.findAll()).thenReturn(List.of(patientEntity));

        List<Patient> result = patientService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDocument()).isEqualTo("1020304050");
        verify(patientRepository).findAll();
    }

    @Test
    @DisplayName("Debe consultar paciente por documento existente")
    void shouldFindPatientByDocument() {
        when(patientRepository.findByDocument("1020304050")).thenReturn(Optional.of(patientEntity));

        Patient result = patientService.findByDocument("1020304050");

        assertThat(result).isNotNull();
        assertThat(result.getDocument()).isEqualTo("1020304050");
        assertThat(result.getFullName()).isEqualTo("Juan Perez");
    }

    @Test
    @DisplayName("Debe lanzar 404 al consultar paciente inexistente")
    void shouldThrowNotFoundWhenPatientDoesNotExist() {
        when(patientRepository.findByDocument("999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.findByDocument("999999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999999");
    }

    @Test
    @DisplayName("Debe crear un paciente exitosamente")
    void shouldCreatePatient() {
        when(patientRepository.existsByDocument("1020304050")).thenReturn(false);
        when(patientRepository.save(any(PatientEntity.class))).thenReturn(patientEntity);

        Patient created = patientService.create(patientModel);

        assertThat(created).isNotNull();
        assertThat(created.getDocument()).isEqualTo("1020304050");
        verify(patientRepository).save(any(PatientEntity.class));
    }

    @Test
    @DisplayName("Debe lanzar 409 al crear paciente con documento duplicado")
    void shouldThrowConflictWhenPatientDocumentAlreadyExists() {
        when(patientRepository.existsByDocument("1020304050")).thenReturn(true);

        assertThatThrownBy(() -> patientService.create(patientModel))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("1020304050");

        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe actualizar paciente existente")
    void shouldUpdatePatient() {
        PatientUpdateDto updateDto = PatientUpdateDto.builder()
                .fullName("Juan Perez Actualizado")
                .birthDate(LocalDate.of(1995, 5, 20))
                .insuranceType(InsuranceType.FULL)
                .build();

        when(patientRepository.findByDocument("1020304050")).thenReturn(Optional.of(patientEntity));
        when(patientRepository.save(any(PatientEntity.class))).thenReturn(patientEntity);

        Patient updated = patientService.update("1020304050", updateDto);

        assertThat(updated).isNotNull();
        verify(patientRepository).save(patientEntity);
    }

    @Test
    @DisplayName("Debe eliminar paciente sin citas SCHEDULED")
    void shouldDeletePatientWithoutScheduledAppointments() {
        when(patientRepository.findByDocument("1020304050")).thenReturn(Optional.of(patientEntity));
        when(appointmentRepository.existsByPatientDocumentAndStatus("1020304050", AppointmentStatus.SCHEDULED)).thenReturn(false);

        patientService.delete("1020304050");

        verify(patientRepository).delete(patientEntity);
    }

    @Test
    @DisplayName("Debe rechazar con 422 eliminar paciente con citas SCHEDULED")
    void shouldThrowUnprocessableEntityWhenDeletingPatientWithScheduledAppointments() {
        when(patientRepository.findByDocument("1020304050")).thenReturn(Optional.of(patientEntity));
        when(appointmentRepository.existsByPatientDocumentAndStatus("1020304050", AppointmentStatus.SCHEDULED)).thenReturn(true);

        assertThatThrownBy(() -> patientService.delete("1020304050"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("citas programadas");

        verify(patientRepository, never()).delete(any());
    }
}
