package com.example.clinica.controllers;

import com.example.clinica.dtos.PatientCreateDto;
import com.example.clinica.dtos.PatientResponseDto;
import com.example.clinica.dtos.PatientUpdateDto;
import com.example.clinica.mappers.PatientMapper;
import com.example.clinica.models.Patient;
import com.example.clinica.services.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    public ResponseEntity<List<PatientResponseDto>> getAllPatients() {
        List<Patient> patients = patientService.findAll();
        List<PatientResponseDto> response = patients.stream()
                .map(PatientMapper::toResponseDto)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{document}")
    public ResponseEntity<PatientResponseDto> getPatientByDocument(@PathVariable String document) {
        Patient patient = patientService.findByDocument(document);
        return ResponseEntity.ok(PatientMapper.toResponseDto(patient));
    }

    @PostMapping
    public ResponseEntity<PatientResponseDto> createPatient(@Valid @RequestBody PatientCreateDto createDto) {
        Patient patient = PatientMapper.toModel(createDto);
        Patient createdPatient = patientService.create(patient);
        return ResponseEntity.status(HttpStatus.CREATED).body(PatientMapper.toResponseDto(createdPatient));
    }

    @PutMapping("/{document}")
    public ResponseEntity<PatientResponseDto> updatePatient(
            @PathVariable String document,
            @Valid @RequestBody PatientUpdateDto updateDto
    ) {
        Patient updatedPatient = patientService.update(document, updateDto);
        return ResponseEntity.ok(PatientMapper.toResponseDto(updatedPatient));
    }

    @DeleteMapping("/{document}")
    public ResponseEntity<Void> deletePatient(@PathVariable String document) {
        patientService.delete(document);
        return ResponseEntity.noContent().build();
    }
}
