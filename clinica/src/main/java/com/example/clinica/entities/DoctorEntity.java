package com.example.clinica.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String license;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String specialty;

    @Column(nullable = false)
    private Double consultationFee;

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<AppointmentEntity> appointments = new ArrayList<>();
}
