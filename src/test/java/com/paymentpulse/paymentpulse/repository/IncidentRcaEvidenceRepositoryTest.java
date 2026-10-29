package com.paymentpulse.paymentpulse.repository;

import com.paymentpulse.paymentpulse.model.IncidentRcaEvidence;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IncidentRcaEvidenceRepositoryTest {

    @Test
    void shouldRetrieveRcaHistoryNewestFirst() {
        IncidentRcaEvidenceRepository repository =
                mock(IncidentRcaEvidenceRepository.class);

        IncidentRcaEvidence newer = new IncidentRcaEvidence();
        newer.setIncidentID("INC-TEST-1");
        newer.setEvaluatedAt(LocalDateTime.of(2026, 10, 26, 10, 5));
        newer.setEvidence("Updated diagnosis");

        IncidentRcaEvidence older = new IncidentRcaEvidence();
        older.setIncidentID("INC-TEST-1");
        older.setEvaluatedAt(LocalDateTime.of(2026, 10, 26, 10, 0));
        older.setEvidence("Initial diagnosis");

        when(repository.findByIncidentIdOrderByEvaluatedAtDesc("INC-TEST-1"))
                .thenReturn(List.of(newer, older));

        List<IncidentRcaEvidence> history =
                repository.findByIncidentIdOrderByEvaluatedAtDesc("INC-TEST-1");

        assertEquals(2, history.size());
        assertEquals("Updated diagnosis", history.get(0).getEvidence());
        assertEquals("Initial diagnosis", history.get(1).getEvidence());

        verify(repository).findByIncidentIdOrderByEvaluatedAtDesc("INC-TEST-1");
    }
}