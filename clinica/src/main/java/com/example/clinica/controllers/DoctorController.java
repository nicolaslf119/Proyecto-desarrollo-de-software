package com.example.clinica.controllers;

import com.example.clinica.dtos.DoctorCreateDto;
import com.example.clinica.dtos.DoctorResponseDto;
import com.example.clinica.dtos.DoctorUpdateDto;
import com.example.clinica.mappers.DoctorMapper;
import com.example.clinica.models.Doctor;
import com.example.clinica.services.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping
    public ResponseEntity<List<DoctorResponseDto>> getAllDoctors() {
        List<Doctor> doctors = doctorService.findAll();
        List<DoctorResponseDto> response = doctors.stream()
                .map(DoctorMapper::toResponseDto)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{license}")
    public ResponseEntity<DoctorResponseDto> getDoctorByLicense(@PathVariable String license) {
        Doctor doctor = doctorService.findByLicense(license);
        return ResponseEntity.ok(DoctorMapper.toResponseDto(doctor));
    }

    @PostMapping
    public ResponseEntity<DoctorResponseDto> createDoctor(@Valid @RequestBody DoctorCreateDto createDto) {
        Doctor doctor = DoctorMapper.toModel(createDto);
        Doctor createdDoctor = doctorService.create(doctor);
        return ResponseEntity.status(HttpStatus.CREATED).body(DoctorMapper.toResponseDto(createdDoctor));
    }

    @PutMapping("/{license}")
    public ResponseEntity<DoctorResponseDto> updateDoctor(
            @PathVariable String license,
            @Valid @RequestBody DoctorUpdateDto updateDto
    ) {
        Doctor updatedDoctor = doctorService.update(license, updateDto);
        return ResponseEntity.ok(DoctorMapper.toResponseDto(updatedDoctor));
    }

    @DeleteMapping("/{license}")
    public ResponseEntity<Void> deleteDoctor(@PathVariable String license) {
        doctorService.delete(license);
        return ResponseEntity.noContent().build();
    }
}
