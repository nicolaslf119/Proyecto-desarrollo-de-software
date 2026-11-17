package com.example.clinica.controllers;

import com.example.clinica.dtos.AppointmentCreateDto;
import com.example.clinica.dtos.AppointmentResponseDto;
import com.example.clinica.mappers.AppointmentMapper;
import com.example.clinica.models.Appointment;
import com.example.clinica.services.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointment")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<AppointmentResponseDto> scheduleAppointment(
            @Valid @RequestBody AppointmentCreateDto createDto
    ) {
        Appointment appointment = appointmentService.scheduleAppointment(
                createDto.getDoctorLicense(),
                createDto.getPatientDocument(),
                createDto.getDateTime()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(AppointmentMapper.toResponseDto(appointment));
    }

    @GetMapping("/{number}")
    public ResponseEntity<AppointmentResponseDto> getAppointmentByNumber(@PathVariable String number) {
        Appointment appointment = appointmentService.findByNumber(number);
        return ResponseEntity.ok(AppointmentMapper.toResponseDto(appointment));
    }

    @PatchMapping("/{number}/cancel")
    public ResponseEntity<AppointmentResponseDto> cancelAppointment(@PathVariable String number) {
        Appointment appointment = appointmentService.cancelAppointment(number);
        return ResponseEntity.ok(AppointmentMapper.toResponseDto(appointment));
    }

    @PatchMapping("/{number}/attend")
    public ResponseEntity<AppointmentResponseDto> attendAppointment(@PathVariable String number) {
        Appointment appointment = appointmentService.attendAppointment(number);
        return ResponseEntity.ok(AppointmentMapper.toResponseDto(appointment));
    }

    @PatchMapping("/{number}/no-show")
    public ResponseEntity<AppointmentResponseDto> noShowAppointment(@PathVariable String number) {
        Appointment appointment = appointmentService.noShowAppointment(number);
        return ResponseEntity.ok(AppointmentMapper.toResponseDto(appointment));
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponseDto>> getAppointmentsByDoctorAndDate(
            @RequestParam String doctorLicense,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<Appointment> appointments = appointmentService.findByDoctorAndDate(doctorLicense, date);
        List<AppointmentResponseDto> response = appointments.stream()
                .map(AppointmentMapper::toResponseDto)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patient/{document}")
    public ResponseEntity<List<AppointmentResponseDto>> getAppointmentHistoryByPatient(
            @PathVariable String document
    ) {
        List<Appointment> appointments = appointmentService.findByPatient(document);
        List<AppointmentResponseDto> response = appointments.stream()
                .map(AppointmentMapper::toResponseDto)
                .toList();
        return ResponseEntity.ok(response);
    }
}
