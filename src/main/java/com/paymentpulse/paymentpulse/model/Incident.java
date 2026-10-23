package com.paymentpulse.paymentpulse.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table
public class Incident {

    @Id
    private String id;
    private String severity;
    private String status;
    private String type;
    private LocalDateTime startedAt;
    private LocalDateTime resolvedAt;
    private double failureRate;
    private double p95Latency;
    private String gatewayId;
    private double gatewayFailureRate;
    private String description;
    @Column(nullable = false )
    private int healthyEvaluations;

    public Incident(){
    }

    public  Incident(String id, String severity, String status, String type, LocalDateTime startedAt, double failureRate, double p95Latency, String gatewayId,
    double gatewayFailureRate, String description) {
        this.id = id;
        this.severity=severity;
        this.status = status;
        this.type=type;
        this.startedAt=startedAt;
        this.failureRate=failureRate;
        this.p95Latency=p95Latency;
        this.gatewayId=gatewayId;
        this.gatewayFailureRate=gatewayFailureRate;
        this.description=description;
    }

    public String getId(){
        return id;
    }
    public void setId(String id){
        this.id=id;
    }

    public String getSeverity(){
        return severity;
    }
    public void setSeverity(String severity){
        this.severity=severity;
    }

    public String getStatus(){
        return status;
    }
    public void setStatus(String status){
        this.status=status;
    }

    public String getType(){
        return type;
    }
    public void setType(String type){
        this.type=type;
    }

    public LocalDateTime getStartedAt(){
        return startedAt;
    }
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public double getFailureRate(){
        return failureRate;
    }
    public void setFailureRate(double failureRate){
        this.failureRate=failureRate;
    }

    public double getP95Latency() {
        return p95Latency;
    }
    public void setP95Latency(double p95Latency) {
        this.p95Latency = p95Latency;
    }

    public String getGatewayId(){
        return gatewayId;
    }
    public void setGatewayId(String gatewayId) {
        this.gatewayId = gatewayId;
    }

    public double getGatewayFailureRate() {
        return gatewayFailureRate;
    }
    public void setGatewayFailureRate(double gatewayFailureRate) {
        this.gatewayFailureRate = gatewayFailureRate;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }
    public void setResolvedAt(LocalDateTime resolvedAt){
        this.resolvedAt=resolvedAt;
    }

    public int getHealthyEvaluations() {
        return healthyEvaluations;
    }
    public void setHealthyEvaluations(int healthyEvaluations){
        this.healthyEvaluations=healthyEvaluations;
    }
}
