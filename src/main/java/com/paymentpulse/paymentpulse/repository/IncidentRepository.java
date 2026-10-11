package com.paymentpulse.paymentpulse.repository;

import com.paymentpulse.paymentpulse.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, String> {
    Optional<Incident> findFirstByStatusOrderByStartedAtDesc(String status);
}
