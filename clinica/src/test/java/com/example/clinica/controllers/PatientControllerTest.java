package com.example.clinica.controllers;

import com.example.clinica.dtos.PatientUpdateDto;
import com.example.clinica.exceptions.BusinessValidationException;
import com.example.clinica.exceptions.GlobalExceptionHandler;
import com.example.clinica.exceptions.ResourceConflictException;
import com.example.clinica.exceptions.ResourceNotFoundException;
import com.example.clinica.models.InsuranceType;
import com.example.clinica.models.Patient;
import com.example.clinica.services.PatientService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PatientService patientService;

    @InjectMocks
    private PatientController patientController;

    private Patient patient;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(patientController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        patient = Patient.builder()
                .id(UUID.randomUUID())
                .document("1020304050")
                .fullName("Juan Perez")
                .birthDate(LocalDate.of(1990, 1, 1))
                .insuranceType(InsuranceType.BASIC)
                .noShowCount(0)
                .build();
    }

    @Test
    @DisplayName("GET /api/patient - Debe listar pacientes")
    void shouldListPatients() throws Exception {
        when(patientService.findAll()).thenReturn(List.of(patient));

        mockMvc.perform(get("/api/patient"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].document").value("1020304050"))
                .andExpect(jsonPath("$[0].fullName").value("Juan Perez"));
    }

    @Test
    @DisplayName("GET /api/patient/{document} - Debe consultar paciente por documento")
    void shouldGetPatientByDocument() throws Exception {
        when(patientService.findByDocument("1020304050")).thenReturn(patient);

        mockMvc.perform(get("/api/patient/1020304050"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document").value("1020304050"));
    }

    @Test
    @DisplayName("GET /api/patient/{document} - Debe retornar 404 si el paciente no existe")
    void shouldReturn404WhenPatientNotFound() throws Exception {
        when(patientService.findByDocument("999999"))
                .thenThrow(new ResourceNotFoundException("Paciente con documento 999999 no encontrado"));

        mockMvc.perform(get("/api/patient/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/patient - Debe retornar 201 al crear paciente exitosamente")
    void shouldCreatePatient() throws Exception {
        String json = """
                {
                    "document": "1020304050",
                    "fullName": "Juan Perez",
                    "birthDate": "1990-01-01",
                    "insuranceType": "BASIC"
                }
                """;

        when(patientService.create(any(Patient.class))).thenReturn(patient);

        mockMvc.perform(post("/api/patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.document").value("1020304050"));
    }

    @Test
    @DisplayName("POST /api/patient - Debe retornar 409 si el documento ya existe")
    void shouldReturn409WhenPatientDocumentExists() throws Exception {
        String json = """
                {
                    "document": "1020304050",
                    "fullName": "Juan Perez",
                    "birthDate": "1990-01-01",
                    "insuranceType": "BASIC"
                }
                """;

        when(patientService.create(any(Patient.class)))
                .thenThrow(new ResourceConflictException("Ya existe un paciente con el documento 1020304050"));

        mockMvc.perform(post("/api/patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("PUT /api/patient/{document} - Debe retornar 200 al actualizar paciente")
    void shouldUpdatePatient() throws Exception {
        String json = """
                {
                    "fullName": "Juan Perez Actualizado",
                    "birthDate": "1990-01-01",
                    "insuranceType": "FULL"
                }
                """;

        when(patientService.update(eq("1020304050"), any(PatientUpdateDto.class))).thenReturn(patient);

        mockMvc.perform(put("/api/patient/1020304050")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/patient/{document} - Debe retornar 204 al eliminar paciente")
    void shouldDeletePatient() throws Exception {
        doNothing().when(patientService).delete("1020304050");

        mockMvc.perform(delete("/api/patient/1020304050"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/patient/{document} - Debe retornar 422 si tiene citas SCHEDULED")
    void shouldReturn422WhenDeletingPatientWithScheduledAppointments() throws Exception {
        doThrow(new BusinessValidationException("No se puede eliminar el paciente porque tiene citas programadas"))
                .when(patientService).delete("1020304050");

        mockMvc.perform(delete("/api/patient/1020304050"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }
}
