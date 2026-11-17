package com.example.clinica.repositories;

import com.example.clinica.entities.AppointmentEntity;
import com.example.clinica.models.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<AppointmentEntity, UUID> {

    Optional<AppointmentEntity> findByNumber(String number);

    boolean existsByNumber(String number);

    // RN4: Un médico no puede tener dos citas SCHEDULED en el mismo horario
    boolean existsByDoctorLicenseAndDateTimeAndStatus(String doctorLicense, LocalDateTime dateTime, AppointmentStatus status);

    // RN5: Un paciente no puede tener dos citas SCHEDULED el mismo día
    boolean existsByPatientDocumentAndDateTimeBetweenAndStatus(
            String patientDocument,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            AppointmentStatus status
    );

    // Consultas Parte 5: Citas de un médico en un día
    List<AppointmentEntity> findByDoctorLicenseAndDateTimeBetweenOrderByDateTimeAsc(
            String doctorLicense,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    // Consultas Parte 5: Historial de citas de un paciente
    List<AppointmentEntity> findByPatientDocumentOrderByDateTimeDesc(String patientDocument);

    // Para validar antes de DELETE Doctor y DELETE Patient
    boolean existsByDoctorLicenseAndStatus(String doctorLicense, AppointmentStatus status);

    boolean existsByPatientDocumentAndStatus(String patientDocument, AppointmentStatus status);
}
