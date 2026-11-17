package com.example.clinica.repositories;

import com.example.clinica.entities.PatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<PatientEntity, UUID> {
    Optional<PatientEntity> findByDocument(String document);
    boolean existsByDocument(String document);
}
