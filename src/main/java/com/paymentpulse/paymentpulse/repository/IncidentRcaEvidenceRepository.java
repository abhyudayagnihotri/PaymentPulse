package com.paymentpulse.paymentpulse.repository;

import com.paymentpulse.paymentpulse.model.IncidentRcaEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRcaEvidenceRepository extends JpaRepository<IncidentRcaEvidence, Long> {
    List<IncidentRcaEvidence> findByIncidentIdOrderByEvaluatedAtDesc(String incidentId);
}
