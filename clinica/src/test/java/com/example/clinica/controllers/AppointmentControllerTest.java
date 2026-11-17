package com.example.clinica.controllers;

import com.example.clinica.exceptions.BusinessValidationException;
import com.example.clinica.exceptions.GlobalExceptionHandler;
import com.example.clinica.exceptions.ResourceConflictException;
import com.example.clinica.exceptions.ResourceNotFoundException;
import com.example.clinica.models.Appointment;
import com.example.clinica.models.AppointmentStatus;
import com.example.clinica.models.Doctor;
import com.example.clinica.models.InsuranceType;
import com.example.clinica.models.Patient;
import com.example.clinica.services.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AppointmentService appointmentService;

    @InjectMocks
    private AppointmentController appointmentController;

    private Appointment appointmentModel;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(appointmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        Doctor doctor = Doctor.builder()
                .id(UUID.randomUUID())
                .license("MED-100")
                .fullName("Dr. House")
                .specialty("Diagnostico")
                .consultationFee(100000.0)
                .build();

        Patient patient = Patient.builder()
                .id(UUID.randomUUID())
                .document("1020304050")
                .fullName("Juan Perez")
                .birthDate(LocalDate.of(1990, 1, 1))
                .insuranceType(InsuranceType.NONE)
                .noShowCount(0)
                .build();

        appointmentModel = Appointment.builder()
                .id(UUID.randomUUID())
                .number("CITA-0001")
                .doctor(doctor)
                .patient(patient)
                .dateTime(LocalDateTime.of(2026, 12, 1, 9, 30, 0))
                .status(AppointmentStatus.SCHEDULED)
                .amountToPay(100000.0)
                .build();
    }

    @Test
    @DisplayName("POST /api/appointment - Debe retornar 201 Created al agendar exitosamente")
    void shouldReturn201WhenScheduledSuccessfully() throws Exception {
        String json = """
                {
                    "doctorLicense": "MED-100",
                    "patientDocument": "1020304050",
                    "dateTime": "2026-12-01T09:30:00"
                }
                """;

        when(appointmentService.scheduleAppointment(eq("MED-100"), eq("1020304050"), any(LocalDateTime.class)))
                .thenReturn(appointmentModel);

        mockMvc.perform(post("/api/appointment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("CITA-0001"))
                .andExpect(jsonPath("$.amountToPay").value(100000.0))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("POST /api/appointment - Debe retornar 409 cuando hay conflicto de horario (RN4)")
    void shouldReturn409WhenDoctorConflict() throws Exception {
        String json = """
                {
                    "doctorLicense": "MED-100",
                    "patientDocument": "1020304050",
                    "dateTime": "2026-12-01T09:30:00"
                }
                """;

        when(appointmentService.scheduleAppointment(any(), any(), any()))
                .thenThrow(new ResourceConflictException("El médico MED-100 ya tiene una cita en ese horario"));

        mockMvc.perform(post("/api/appointment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("El médico MED-100 ya tiene una cita en ese horario"));
    }

    @Test
    @DisplayName("POST /api/appointment - Debe retornar 422 cuando la cita incumple regla de agenda (RN1, RN2, etc.)")
    void shouldReturn422WhenBusinessRuleFails() throws Exception {
        String json = """
                {
                    "doctorLicense": "MED-100",
                    "patientDocument": "1020304050",
                    "dateTime": "2026-12-01T09:15:00"
                }
                """;

        when(appointmentService.scheduleAppointment(any(), any(), any()))
                .thenThrow(new BusinessValidationException("Las citas sólo se agendan en horas en punto o y media (:00 o :30)"));

        mockMvc.perform(post("/api/appointment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"));
    }

    @Test
    @DisplayName("GET /api/appointment/{number} - Debe retornar 200 y la cita encontrada")
    void shouldReturn200WhenAppointmentFound() throws Exception {
        when(appointmentService.findByNumber("CITA-0001")).thenReturn(appointmentModel);

        mockMvc.perform(get("/api/appointment/CITA-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("CITA-0001"));
    }

    @Test
    @DisplayName("GET /api/appointment/{number} - Debe retornar 404 cuando la cita no existe")
    void shouldReturn404WhenAppointmentNotFound() throws Exception {
        when(appointmentService.findByNumber("CITA-9999"))
                .thenThrow(new ResourceNotFoundException("Cita con número CITA-9999 no encontrada"));

        mockMvc.perform(get("/api/appointment/CITA-9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PATCH /api/appointment/{number}/cancel - Debe retornar 200 al cancelar")
    void shouldReturn200WhenCancelled() throws Exception {
        appointmentModel.setStatus(AppointmentStatus.CANCELLED);
        when(appointmentService.cancelAppointment("CITA-0001")).thenReturn(appointmentModel);

        mockMvc.perform(patch("/api/appointment/CITA-0001/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("PATCH /api/appointment/{number}/attend - Debe retornar 200 al atender")
    void shouldReturn200WhenAttended() throws Exception {
        appointmentModel.setStatus(AppointmentStatus.ATTENDED);
        when(appointmentService.attendAppointment("CITA-0001")).thenReturn(appointmentModel);

        mockMvc.perform(patch("/api/appointment/CITA-0001/attend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATTENDED"));
    }

    @Test
    @DisplayName("PATCH /api/appointment/{number}/no-show - Debe retornar 200 al marcar inasistencia")
    void shouldReturn200WhenNoShow() throws Exception {
        appointmentModel.setStatus(AppointmentStatus.NO_SHOW);
        when(appointmentService.noShowAppointment("CITA-0001")).thenReturn(appointmentModel);

        mockMvc.perform(patch("/api/appointment/CITA-0001/no-show"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_SHOW"));
    }

    @Test
    @DisplayName("GET /api/appointment?doctorLicense=&date= - Debe retornar 200 y lista de citas")
    void shouldReturnAppointmentsByDoctorAndDate() throws Exception {
        when(appointmentService.findByDoctorAndDate(eq("MED-100"), any(LocalDate.class)))
                .thenReturn(List.of(appointmentModel));

        mockMvc.perform(get("/api/appointment")
                        .param("doctorLicense", "MED-100")
                        .param("date", "2026-12-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("CITA-0001"));
    }

    @Test
    @DisplayName("GET /api/appointment/patient/{document} - Debe retornar 200 y lista histórica")
    void shouldReturnAppointmentHistoryByPatient() throws Exception {
        when(appointmentService.findByPatient("1020304050"))
                .thenReturn(List.of(appointmentModel));

        mockMvc.perform(get("/api/appointment/patient/1020304050"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("CITA-0001"));
    }
}
