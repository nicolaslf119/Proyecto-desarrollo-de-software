package com.example.clinica.services;

import com.example.clinica.entities.AppointmentEntity;
import com.example.clinica.entities.DoctorEntity;
import com.example.clinica.entities.PatientEntity;
import com.example.clinica.exceptions.BusinessValidationException;
import com.example.clinica.exceptions.ResourceConflictException;
import com.example.clinica.exceptions.ResourceNotFoundException;
import com.example.clinica.mappers.AppointmentMapper;
import com.example.clinica.models.Appointment;
import com.example.clinica.models.AppointmentStatus;
import com.example.clinica.models.InsuranceType;
import com.example.clinica.repositories.AppointmentRepository;
import com.example.clinica.repositories.DoctorRepository;
import com.example.clinica.repositories.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    @Transactional
    public Appointment scheduleAppointment(String doctorLicense, String patientDocument, LocalDateTime dateTime) {
        // RN1: Las citas duran 30 min y sólo se agendan en horas en punto o y media (:00 o :30)
        validateMinutes(dateTime);

        // RN2: Sólo se agenda de lunes a viernes, entre 8:00 y 17:00 (la última cita inicia 16:30)
        validateWorkingDaysAndHours(dateTime);

        // RN3: No se puede agendar en el pasado
        validateNotPast(dateTime);

        // Verificar existencia de Doctor y Patient
        DoctorEntity doctor = doctorRepository.findByLicense(doctorLicense)
                .orElseThrow(() -> new ResourceNotFoundException("Médico con licencia " + doctorLicense + " no encontrado"));

        PatientEntity patient = patientRepository.findByDocument(patientDocument)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente con documento " + patientDocument + " no encontrado"));

        // RN7: Un paciente con 2 o más inasistencias (NO_SHOW) no puede agendar
        if (patient.getNoShowCount() >= 2) {
            throw new BusinessValidationException("Un paciente con 2 o más inasistencias no puede agendar citas");
        }

        // RN4: Un médico no puede tener dos citas SCHEDULED en el mismo horario
        if (appointmentRepository.existsByDoctorLicenseAndDateTimeAndStatus(doctorLicense, dateTime, AppointmentStatus.SCHEDULED)) {
            throw new ResourceConflictException("El médico " + doctorLicense + " ya tiene una cita en ese horario");
        }

        // RN5: Un paciente no puede tener dos citas SCHEDULED el mismo día
        LocalDateTime startOfDay = dateTime.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = dateTime.toLocalDate().atTime(23, 59, 59, 999999999);
        if (appointmentRepository.existsByPatientDocumentAndDateTimeBetweenAndStatus(patientDocument, startOfDay, endOfDay, AppointmentStatus.SCHEDULED)) {
            throw new ResourceConflictException("El paciente " + patientDocument + " ya tiene una cita programada para ese día");
        }

        // RN6: Valor a pagar según seguro: NONE 100%, BASIC 50%, FULL 0% de consultationFee
        double amountToPay = calculateAmountToPay(doctor.getConsultationFee(), patient.getInsuranceType());

        // Generar número de cita único
        String appointmentNumber = generateUniqueAppointmentNumber();

        AppointmentEntity appointmentEntity = AppointmentEntity.builder()
                .number(appointmentNumber)
                .doctor(doctor)
                .patient(patient)
                .dateTime(dateTime)
                .status(AppointmentStatus.SCHEDULED)
                .amountToPay(amountToPay)
                .build();

        AppointmentEntity saved = appointmentRepository.save(appointmentEntity);
        return AppointmentMapper.toModel(saved);
    }

    @Transactional(readOnly = true)
    public Appointment findByNumber(String number) {
        return appointmentRepository.findByNumber(number)
                .map(AppointmentMapper::toModel)
                .orElseThrow(() -> new ResourceNotFoundException("Cita con número " + number + " no encontrada"));
    }

    @Transactional
    public Appointment cancelAppointment(String number) {
        AppointmentEntity appointment = appointmentRepository.findByNumber(number)
                .orElseThrow(() -> new ResourceNotFoundException("Cita con número " + number + " no encontrada"));

        // Máquina de estados: sólo SCHEDULED puede pasar a CANCELLED
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BusinessValidationException("Sólo se pueden cancelar citas en estado SCHEDULED");
        }

        // RN8: Cancelar sólo es posible si faltan más de 2 horas para la cita y está SCHEDULED
        Duration duration = Duration.between(LocalDateTime.now(), appointment.getDateTime());
        if (duration.compareTo(Duration.ofHours(2)) <= 0) {
            throw new BusinessValidationException("Cancelar sólo es posible si faltan más de 2 horas para la cita");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        AppointmentEntity updated = appointmentRepository.save(appointment);
        return AppointmentMapper.toModel(updated);
    }

    @Transactional
    public Appointment attendAppointment(String number) {
        AppointmentEntity appointment = appointmentRepository.findByNumber(number)
                .orElseThrow(() -> new ResourceNotFoundException("Cita con número " + number + " no encontrada"));

        // Máquina de estados: sólo SCHEDULED puede pasar a ATTENDED
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BusinessValidationException("Sólo se pueden atender citas en estado SCHEDULED");
        }

        appointment.setStatus(AppointmentStatus.ATTENDED);
        AppointmentEntity updated = appointmentRepository.save(appointment);
        return AppointmentMapper.toModel(updated);
    }

    @Transactional
    public Appointment noShowAppointment(String number) {
        AppointmentEntity appointment = appointmentRepository.findByNumber(number)
                .orElseThrow(() -> new ResourceNotFoundException("Cita con número " + number + " no encontrada"));

        // Máquina de estados: sólo SCHEDULED puede pasar a NO_SHOW
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BusinessValidationException("Sólo se puede marcar inasistencia en citas en estado SCHEDULED");
        }

        appointment.setStatus(AppointmentStatus.NO_SHOW);

        // Al marcar NO_SHOW se incrementa noShowCount del paciente (RN7)
        PatientEntity patient = appointment.getPatient();
        patient.setNoShowCount(patient.getNoShowCount() + 1);
        patientRepository.save(patient);

        AppointmentEntity updated = appointmentRepository.save(appointment);
        return AppointmentMapper.toModel(updated);
    }

    @Transactional(readOnly = true)
    public List<Appointment> findByDoctorAndDate(String doctorLicense, LocalDate date) {
        if (!doctorRepository.existsByLicense(doctorLicense)) {
            throw new ResourceNotFoundException("Médico con licencia " + doctorLicense + " no encontrado");
        }
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59, 999999999);
        return appointmentRepository.findByDoctorLicenseAndDateTimeBetweenOrderByDateTimeAsc(doctorLicense, start, end).stream()
                .map(AppointmentMapper::toModel)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Appointment> findByPatient(String document) {
        if (!patientRepository.existsByDocument(document)) {
            throw new ResourceNotFoundException("Paciente con documento " + document + " no encontrado");
        }
        return appointmentRepository.findByPatientDocumentOrderByDateTimeDesc(document).stream()
                .map(AppointmentMapper::toModel)
                .toList();
    }

    private void validateMinutes(LocalDateTime dateTime) {
        int minute = dateTime.getMinute();
        if ((minute != 0 && minute != 30) || dateTime.getSecond() != 0 || dateTime.getNano() != 0) {
            throw new BusinessValidationException("Las citas duran 30 min y sólo se agendan en horas en punto o y media (:00 o :30)");
        }
    }

    private void validateWorkingDaysAndHours(LocalDateTime dateTime) {
        DayOfWeek day = dateTime.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            throw new BusinessValidationException("Sólo se agenda de lunes a viernes");
        }

        LocalTime time = dateTime.toLocalTime();
        if (time.isBefore(LocalTime.of(8, 0)) || time.isAfter(LocalTime.of(16, 30))) {
            throw new BusinessValidationException("Sólo se agenda entre 8:00 y 17:00 (la última cita inicia 16:30)");
        }
    }

    private void validateNotPast(LocalDateTime dateTime) {
        if (dateTime.isBefore(LocalDateTime.now())) {
            throw new BusinessValidationException("No se puede agendar en el pasado");
        }
    }

    private double calculateAmountToPay(double fee, InsuranceType insuranceType) {
        if (insuranceType == null) {
            return fee;
        }
        return switch (insuranceType) {
            case NONE -> fee;
            case BASIC -> fee * 0.5;
            case FULL -> 0.0;
        };
    }

    private String generateUniqueAppointmentNumber() {
        String number;
        do {
            number = "CITA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (appointmentRepository.existsByNumber(number));
        return number;
    }
}
