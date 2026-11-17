package com.example.clinica.services;

import com.example.clinica.entities.AppointmentEntity;
import com.example.clinica.entities.DoctorEntity;
import com.example.clinica.entities.PatientEntity;
import com.example.clinica.exceptions.BusinessValidationException;
import com.example.clinica.exceptions.ResourceConflictException;
import com.example.clinica.exceptions.ResourceNotFoundException;
import com.example.clinica.models.Appointment;
import com.example.clinica.models.AppointmentStatus;
import com.example.clinica.models.InsuranceType;
import com.example.clinica.repositories.AppointmentRepository;
import com.example.clinica.repositories.DoctorRepository;
import com.example.clinica.repositories.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private DoctorEntity doctor;
    private PatientEntity patientNone;
    private PatientEntity patientBasic;
    private PatientEntity patientFull;
    private LocalDateTime futureMonday9Am;

    @BeforeEach
    void setUp() {
        doctor = DoctorEntity.builder()
                .id(UUID.randomUUID())
                .license("MED-100")
                .fullName("Dr. House")
                .specialty("Diagnóstico")
                .consultationFee(100000.0)
                .build();

        patientNone = PatientEntity.builder()
                .id(UUID.randomUUID())
                .document("P-1")
                .fullName("Paciente Uno")
                .birthDate(LocalDate.of(1990, 1, 1))
                .insuranceType(InsuranceType.NONE)
                .noShowCount(0)
                .build();

        patientBasic = PatientEntity.builder()
                .id(UUID.randomUUID())
                .document("P-2")
                .fullName("Paciente Dos")
                .birthDate(LocalDate.of(1992, 2, 2))
                .insuranceType(InsuranceType.BASIC)
                .noShowCount(0)
                .build();

        patientFull = PatientEntity.builder()
                .id(UUID.randomUUID())
                .document("P-3")
                .fullName("Paciente Tres")
                .birthDate(LocalDate.of(1995, 3, 3))
                .insuranceType(InsuranceType.FULL)
                .noShowCount(0)
                .build();

        // Encontrar el siguiente lunes futuro a las 09:00
        LocalDate nextMonday = LocalDate.now().plusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        futureMonday9Am = nextMonday.atTime(9, 0, 0);
    }

    // ==========================================
    // RN1: Horas en punto o y media (:00 o :30)
    // ==========================================
    @Test
    @DisplayName("RN1: Debe lanzar 422 cuando el minuto no es :00 ni :30 (ej. 09:15)")
    void shouldThrow422WhenMinuteIsInvalid() {
        LocalDateTime invalidTime = futureMonday9Am.withMinute(15);

        assertThatThrownBy(() -> appointmentService.scheduleAppointment("MED-100", "P-1", invalidTime))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining(":00 o :30");

        verifyNoInteractions(doctorRepository, patientRepository, appointmentRepository);
    }

    // ==========================================
    // RN2: Lunes a viernes entre 8:00 y 17:00 (última 16:30)
    // ==========================================
    @Test
    @DisplayName("RN2: Debe lanzar 422 cuando es fin de semana (sábado)")
    void shouldThrow422WhenDayIsSaturday() {
        LocalDate saturday = futureMonday9Am.toLocalDate().with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        LocalDateTime saturdayTime = saturday.atTime(9, 0, 0);

        assertThatThrownBy(() -> appointmentService.scheduleAppointment("MED-100", "P-1", saturdayTime))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("lunes a viernes");
    }

    @Test
    @DisplayName("RN2: Debe lanzar 422 cuando la cita es fuera de horario (17:00 o antes de las 08:00)")
    void shouldThrow422WhenTimeIsOutOfRange() {
        LocalDateTime at1700 = futureMonday9Am.withHour(17).withMinute(0);
        LocalDateTime at0730 = futureMonday9Am.withHour(7).withMinute(30);

        assertThatThrownBy(() -> appointmentService.scheduleAppointment("MED-100", "P-1", at1700))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("16:30");

        assertThatThrownBy(() -> appointmentService.scheduleAppointment("MED-100", "P-1", at0730))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("16:30");
    }

    // ==========================================
    // RN3: No se puede agendar en el pasado
    // ==========================================
    @Test
    @DisplayName("RN3: Debe lanzar 422 cuando la fecha es en el pasado")
    void shouldThrow422WhenDateIsInThePast() {
        LocalDate lastMonday = LocalDate.now().minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime pastDate = lastMonday.atTime(9, 0, 0);

        assertThatThrownBy(() -> appointmentService.scheduleAppointment("MED-100", "P-1", pastDate))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("pasado");
    }

    // ==========================================
    // Validar médico y paciente existentes
    // ==========================================
    @Test
    @DisplayName("Debe lanzar 404 cuando el médico no existe")
    void shouldThrow404WhenDoctorDoesNotExist() {
        when(doctorRepository.findByLicense("MED-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.scheduleAppointment("MED-999", "P-1", futureMonday9Am))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("MED-999");
    }

    @Test
    @DisplayName("Debe lanzar 404 cuando el paciente no existe")
    void shouldThrow404WhenPatientDoesNotExist() {
        when(doctorRepository.findByLicense("MED-100")).thenReturn(Optional.of(doctor));
        when(patientRepository.findByDocument("P-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.scheduleAppointment("MED-100", "P-999", futureMonday9Am))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("P-999");
    }

    // ==========================================
    // RN4: Médico no puede tener dos citas SCHEDULED en el mismo horario
    // ==========================================
    @Test
    @DisplayName("RN4: Debe lanzar 409 cuando el médico ya tiene una cita SCHEDULED en ese horario")
    void shouldThrow409WhenDoctorIsBusy() {
        when(doctorRepository.findByLicense("MED-100")).thenReturn(Optional.of(doctor));
        when(patientRepository.findByDocument("P-1")).thenReturn(Optional.of(patientNone));
        when(appointmentRepository.existsByDoctorLicenseAndDateTimeAndStatus("MED-100", futureMonday9Am, AppointmentStatus.SCHEDULED))
                .thenReturn(true);

        assertThatThrownBy(() -> appointmentService.scheduleAppointment("MED-100", "P-1", futureMonday9Am))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("ya tiene una cita");
    }

    // ==========================================
    // RN5: Paciente no puede tener dos citas SCHEDULED el mismo día
    // ==========================================
    @Test
    @DisplayName("RN5: Debe lanzar 409 cuando el paciente ya tiene otra cita SCHEDULED el mismo día")
    void shouldThrow409WhenPatientHasAnotherAppointmentOnTheSameDay() {
        when(doctorRepository.findByLicense("MED-100")).thenReturn(Optional.of(doctor));
        when(patientRepository.findByDocument("P-1")).thenReturn(Optional.of(patientNone));
        when(appointmentRepository.existsByDoctorLicenseAndDateTimeAndStatus("MED-100", futureMonday9Am, AppointmentStatus.SCHEDULED))
                .thenReturn(false);
        when(appointmentRepository.existsByPatientDocumentAndDateTimeBetweenAndStatus(eq("P-1"), any(), any(), eq(AppointmentStatus.SCHEDULED)))
                .thenReturn(true);

        assertThatThrownBy(() -> appointmentService.scheduleAppointment("MED-100", "P-1", futureMonday9Am))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("ya tiene una cita");
    }

    // ==========================================
    // RN6: Tarifas según tipo de seguro
    // ==========================================
    @Test
    @DisplayName("RN6: Seguro NONE paga 100% de la tarifa (100.000)")
    void shouldCalculateAmountToPayForInsuranceNone() {
        when(doctorRepository.findByLicense("MED-100")).thenReturn(Optional.of(doctor));
        when(patientRepository.findByDocument("P-1")).thenReturn(Optional.of(patientNone));
        when(appointmentRepository.existsByDoctorLicenseAndDateTimeAndStatus(any(), any(), any())).thenReturn(false);
        when(appointmentRepository.existsByPatientDocumentAndDateTimeBetweenAndStatus(any(), any(), any(), any())).thenReturn(false);
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment appointment = appointmentService.scheduleAppointment("MED-100", "P-1", futureMonday9Am);

        assertThat(appointment).isNotNull();
        assertThat(appointment.getAmountToPay()).isEqualTo(100000.0);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    @DisplayName("RN6: Seguro BASIC paga 50% de la tarifa (50.000)")
    void shouldCalculateAmountToPayForInsuranceBasic() {
        when(doctorRepository.findByLicense("MED-100")).thenReturn(Optional.of(doctor));
        when(patientRepository.findByDocument("P-2")).thenReturn(Optional.of(patientBasic));
        when(appointmentRepository.existsByDoctorLicenseAndDateTimeAndStatus(any(), any(), any())).thenReturn(false);
        when(appointmentRepository.existsByPatientDocumentAndDateTimeBetweenAndStatus(any(), any(), any(), any())).thenReturn(false);
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime time = futureMonday9Am.withMinute(30);
        Appointment appointment = appointmentService.scheduleAppointment("MED-100", "P-2", time);

        assertThat(appointment).isNotNull();
        assertThat(appointment.getAmountToPay()).isEqualTo(50000.0);
    }

    @Test
    @DisplayName("RN6: Seguro FULL paga 0% de la tarifa (0.0)")
    void shouldCalculateAmountToPayForInsuranceFull() {
        when(doctorRepository.findByLicense("MED-100")).thenReturn(Optional.of(doctor));
        when(patientRepository.findByDocument("P-3")).thenReturn(Optional.of(patientFull));
        when(appointmentRepository.existsByDoctorLicenseAndDateTimeAndStatus(any(), any(), any())).thenReturn(false);
        when(appointmentRepository.existsByPatientDocumentAndDateTimeBetweenAndStatus(any(), any(), any(), any())).thenReturn(false);
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime time = futureMonday9Am.withHour(10).withMinute(0);
        Appointment appointment = appointmentService.scheduleAppointment("MED-100", "P-3", time);

        assertThat(appointment).isNotNull();
        assertThat(appointment.getAmountToPay()).isEqualTo(0.0);
    }

    // ==========================================
    // RN7: Paciente con 2 o más inasistencias no puede agendar
    // ==========================================
    @Test
    @DisplayName("RN7: Debe lanzar 422 cuando el paciente tiene 2 o más no-shows")
    void shouldThrow422WhenPatientHasTwoOrMoreNoShows() {
        patientNone.setNoShowCount(2);

        when(doctorRepository.findByLicense("MED-100")).thenReturn(Optional.of(doctor));
        when(patientRepository.findByDocument("P-1")).thenReturn(Optional.of(patientNone));

        assertThatThrownBy(() -> appointmentService.scheduleAppointment("MED-100", "P-1", futureMonday9Am))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("2 o más inasistencias");
    }

    // ==========================================
    // RN8: Cancelación de citas
    // ==========================================
    @Test
    @DisplayName("RN8: Debe cancelar cita con más de 2 horas de anticipación en estado SCHEDULED")
    void shouldCancelAppointmentWithMoreThan2HoursNotice() {
        AppointmentEntity entity = AppointmentEntity.builder()
                .id(UUID.randomUUID())
                .number("CITA-001")
                .doctor(doctor)
                .patient(patientNone)
                .dateTime(LocalDateTime.now().plusHours(2).plusSeconds(30))
                .status(AppointmentStatus.SCHEDULED)
                .amountToPay(100000.0)
                .build();

        when(appointmentRepository.findByNumber("CITA-001")).thenReturn(Optional.of(entity));
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment cancelled = appointmentService.cancelAppointment("CITA-001");

        assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("RN8: Debe lanzar 422 cuando se cancela con menos de 2 horas de anticipación (ej. 1 hora)")
    void shouldThrow422WhenCancellingWithLessThan2HoursNotice() {
        AppointmentEntity entity = AppointmentEntity.builder()
                .id(UUID.randomUUID())
                .number("CITA-001")
                .doctor(doctor)
                .patient(patientNone)
                .dateTime(LocalDateTime.now().plusHours(1))
                .status(AppointmentStatus.SCHEDULED)
                .amountToPay(100000.0)
                .build();

        when(appointmentRepository.findByNumber("CITA-001")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> appointmentService.cancelAppointment("CITA-001"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("más de 2 horas");
    }

    @Test
    @DisplayName("RN8: Debe lanzar 422 al cancelar cita no SCHEDULED (ya cancelada)")
    void shouldThrow422WhenCancellingNonScheduledAppointment() {
        AppointmentEntity entity = AppointmentEntity.builder()
                .id(UUID.randomUUID())
                .number("CITA-001")
                .doctor(doctor)
                .patient(patientNone)
                .dateTime(LocalDateTime.now().plusHours(5))
                .status(AppointmentStatus.CANCELLED)
                .amountToPay(100000.0)
                .build();

        when(appointmentRepository.findByNumber("CITA-001")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> appointmentService.cancelAppointment("CITA-001"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("SCHEDULED");
    }

    // ==========================================
    // Máquina de estados: ATTENDED y NO_SHOW
    // ==========================================
    @Test
    @DisplayName("Debe marcar cita como atendida (ATTENDED)")
    void shouldAttendAppointment() {
        AppointmentEntity entity = AppointmentEntity.builder()
                .id(UUID.randomUUID())
                .number("CITA-001")
                .doctor(doctor)
                .patient(patientNone)
                .dateTime(futureMonday9Am)
                .status(AppointmentStatus.SCHEDULED)
                .amountToPay(100000.0)
                .build();

        when(appointmentRepository.findByNumber("CITA-001")).thenReturn(Optional.of(entity));
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment attended = appointmentService.attendAppointment("CITA-001");

        assertThat(attended.getStatus()).isEqualTo(AppointmentStatus.ATTENDED);
    }

    @Test
    @DisplayName("Debe lanzar 422 al intentar atender una cita ya cancelada")
    void shouldThrow422WhenAttendingCancelledAppointment() {
        AppointmentEntity entity = AppointmentEntity.builder()
                .id(UUID.randomUUID())
                .number("CITA-001")
                .doctor(doctor)
                .patient(patientNone)
                .dateTime(futureMonday9Am)
                .status(AppointmentStatus.CANCELLED)
                .amountToPay(100000.0)
                .build();

        when(appointmentRepository.findByNumber("CITA-001")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> appointmentService.attendAppointment("CITA-001"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("SCHEDULED");
    }

    @Test
    @DisplayName("Debe marcar inasistencia (NO_SHOW) e incrementar noShowCount del paciente")
    void shouldMarkNoShowAndIncrementPatientCount() {
        AppointmentEntity entity = AppointmentEntity.builder()
                .id(UUID.randomUUID())
                .number("CITA-001")
                .doctor(doctor)
                .patient(patientNone)
                .dateTime(futureMonday9Am)
                .status(AppointmentStatus.SCHEDULED)
                .amountToPay(100000.0)
                .build();

        when(appointmentRepository.findByNumber("CITA-001")).thenReturn(Optional.of(entity));
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment noShowAppointment = appointmentService.noShowAppointment("CITA-001");

        assertThat(noShowAppointment.getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
        assertThat(patientNone.getNoShowCount()).isEqualTo(1);
        verify(patientRepository).save(patientNone);
    }

    // ==========================================
    // Consultas Parte 5
    // ==========================================
    @Test
    @DisplayName("Parte 5: Consultar citas de un médico en un día")
    void shouldFindAppointmentsByDoctorAndDate() {
        AppointmentEntity entity = AppointmentEntity.builder()
                .id(UUID.randomUUID())
                .number("CITA-001")
                .doctor(doctor)
                .patient(patientNone)
                .dateTime(futureMonday9Am)
                .status(AppointmentStatus.SCHEDULED)
                .amountToPay(100000.0)
                .build();

        when(doctorRepository.existsByLicense("MED-100")).thenReturn(true);
        when(appointmentRepository.findByDoctorLicenseAndDateTimeBetweenOrderByDateTimeAsc(eq("MED-100"), any(), any()))
                .thenReturn(List.of(entity));

        List<Appointment> list = appointmentService.findByDoctorAndDate("MED-100", futureMonday9Am.toLocalDate());

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getNumber()).isEqualTo("CITA-001");
    }

    @Test
    @DisplayName("Parte 5: Consultar historial de citas de un paciente")
    void shouldFindAppointmentHistoryByPatient() {
        AppointmentEntity entity = AppointmentEntity.builder()
                .id(UUID.randomUUID())
                .number("CITA-001")
                .doctor(doctor)
                .patient(patientNone)
                .dateTime(futureMonday9Am)
                .status(AppointmentStatus.SCHEDULED)
                .amountToPay(100000.0)
                .build();

        when(patientRepository.existsByDocument("P-1")).thenReturn(true);
        when(appointmentRepository.findByPatientDocumentOrderByDateTimeDesc("P-1"))
                .thenReturn(List.of(entity));

        List<Appointment> list = appointmentService.findByPatient("P-1");

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getNumber()).isEqualTo("CITA-001");
    }
}
