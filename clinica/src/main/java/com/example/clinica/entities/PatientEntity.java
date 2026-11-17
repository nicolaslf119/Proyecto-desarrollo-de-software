package com.example.clinica.entities;

import com.example.clinica.models.InsuranceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String document;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InsuranceType insuranceType;

    @Column(nullable = false)
    @Builder.Default
    private int noShowCount = 0;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<AppointmentEntity> appointments = new ArrayList<>();
}
