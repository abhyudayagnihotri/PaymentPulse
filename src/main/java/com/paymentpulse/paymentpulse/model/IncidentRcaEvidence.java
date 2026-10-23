package com.paymentpulse.paymentpulse.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident_rca_evidence")
public class IncidentRcaEvidence {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id",nullable=false)
    private String incidentId;
    private LocalDateTime evaluatedAt;
    private String suspectedGateway;
    private String suspectedBank;
    private Double pairFailureRate;
    private Double overallFailureRate;
    private Double failureRateGap;
    private Integer pairTransactions;
    private Integer pairFailures;
    private String confidence;

    @Column(columnDefinition="TEXT")
    private String evidence;

    public IncidentRcaEvidence() {
    }

    public Long getId(){
        return id;
    }

    public String getIncidentID(){
        return incidentId;
    }
    public void setIncidentID(String incidentID) {
        this.incidentId = incidentID;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setSuspectedGateway(String suspectedGateway) {
        this.suspectedGateway = suspectedGateway;
    }
    public String getSuspectedGateway() {
        return suspectedGateway;
    }

    public void setSuspectedBank(String suspectedBank) {
        this.suspectedBank = suspectedBank;
    }
    public String getSuspectedBank() {
        return suspectedBank;
    }

    public void setPairFailureRate(Double pairFailureRate) {
        this.pairFailureRate = pairFailureRate;
    }
    public Double getPairFailureRate() {
        return pairFailureRate;
    }

    public void setOverallFailureRate(Double overallFailureRate) {
        this.overallFailureRate = overallFailureRate;
    }
    public Double getOverallFailureRate() {
        return overallFailureRate;
    }

    public void setFailureRateGap(Double failureRateGap) {
        this.failureRateGap = failureRateGap;
    }
    public Double getFailureRateGap() {
        return failureRateGap;
    }


    public void setPairFailures(Integer pairFailures) {
        this.pairFailures = pairFailures;
    }
    public Integer getPairFailures() {
        return pairFailures;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }
    public String getConfidence() {
        return confidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }
    public String getEvidence() {
        return evidence;
    }

    public void setPairTransactions(Integer pairTransactions) {
        this.pairTransactions = pairTransactions;
    }
    public Integer getPairTransactions() {
        return pairTransactions;
    }
}
