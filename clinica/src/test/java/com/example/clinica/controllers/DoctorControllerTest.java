package com.example.clinica.controllers;

import com.example.clinica.dtos.DoctorUpdateDto;
import com.example.clinica.exceptions.BusinessValidationException;
import com.example.clinica.exceptions.GlobalExceptionHandler;
import com.example.clinica.exceptions.ResourceConflictException;
import com.example.clinica.exceptions.ResourceNotFoundException;
import com.example.clinica.models.Doctor;
import com.example.clinica.services.DoctorService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DoctorControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DoctorService doctorService;

    @InjectMocks
    private DoctorController doctorController;

    private Doctor doctor;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(doctorController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        doctor = Doctor.builder()
                .id(UUID.randomUUID())
                .license("MED-100")
                .fullName("Dr. Gregory House")
                .specialty("Diagnostico")
                .consultationFee(100000.0)
                .build();
    }

    @Test
    @DisplayName("GET /api/doctor - Debe listar médicos")
    void shouldListDoctors() throws Exception {
        when(doctorService.findAll()).thenReturn(List.of(doctor));

        mockMvc.perform(get("/api/doctor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].license").value("MED-100"))
                .andExpect(jsonPath("$[0].fullName").value("Dr. Gregory House"));
    }

    @Test
    @DisplayName("GET /api/doctor/{license} - Debe consultar médico por licencia")
    void shouldGetDoctorByLicense() throws Exception {
        when(doctorService.findByLicense("MED-100")).thenReturn(doctor);

        mockMvc.perform(get("/api/doctor/MED-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.license").value("MED-100"));
    }

    @Test
    @DisplayName("GET /api/doctor/{license} - Debe retornar 404 si el médico no existe")
    void shouldReturn404WhenDoctorNotFound() throws Exception {
        when(doctorService.findByLicense("MED-999"))
                .thenThrow(new ResourceNotFoundException("Médico con licencia MED-999 no encontrado"));

        mockMvc.perform(get("/api/doctor/MED-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/doctor - Debe retornar 201 al crear médico exitosamente")
    void shouldCreateDoctor() throws Exception {
        String json = """
                {
                    "license": "MED-100",
                    "fullName": "Dr. Gregory House",
                    "specialty": "Diagnostico",
                    "consultationFee": 100000.0
                }
                """;

        when(doctorService.create(any(Doctor.class))).thenReturn(doctor);

        mockMvc.perform(post("/api/doctor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.license").value("MED-100"));
    }

    @Test
    @DisplayName("POST /api/doctor - Debe retornar 409 si la licencia ya existe")
    void shouldReturn409WhenDoctorLicenseExists() throws Exception {
        String json = """
                {
                    "license": "MED-100",
                    "fullName": "Dr. Gregory House",
                    "specialty": "Diagnostico",
                    "consultationFee": 100000.0
                }
                """;

        when(doctorService.create(any(Doctor.class)))
                .thenThrow(new ResourceConflictException("Ya existe un médico con la licencia MED-100"));

        mockMvc.perform(post("/api/doctor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("PUT /api/doctor/{license} - Debe retornar 200 al actualizar médico")
    void shouldUpdateDoctor() throws Exception {
        String json = """
                {
                    "fullName": "Dr. House Modificado",
                    "specialty": "Infectologia",
                    "consultationFee": 120000.0
                }
                """;

        when(doctorService.update(eq("MED-100"), any(DoctorUpdateDto.class))).thenReturn(doctor);

        mockMvc.perform(put("/api/doctor/MED-100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/doctor/{license} - Debe retornar 204 No Content al eliminar")
    void shouldDeleteDoctor() throws Exception {
        doNothing().when(doctorService).delete("MED-100");

        mockMvc.perform(delete("/api/doctor/MED-100"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/doctor/{license} - Debe retornar 422 si tiene citas SCHEDULED")
    void shouldReturn422WhenDeletingDoctorWithScheduledAppointments() throws Exception {
        doThrow(new BusinessValidationException("No se puede eliminar el médico porque tiene citas programadas"))
                .when(doctorService).delete("MED-100");

        mockMvc.perform(delete("/api/doctor/MED-100"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }
}
