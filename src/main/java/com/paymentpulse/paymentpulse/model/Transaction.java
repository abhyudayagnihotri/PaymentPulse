package com.paymentpulse.paymentpulse.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;
    private String merchantId;
    private String bankId;
    private String gatewayId;
    private Double amount;
    private String location;
    private String paymentMethod;
    private String status;
    private String responseCode;

    private Integer latencyMs;

    private LocalDateTime timestamp;

    public Transaction(){
    }

    public Long getId(){
        return id;
    }

    public String getMerchantId(){
        return merchantId;
    }
    public void setMerchantId(String merchantId) {
        this.merchantId=merchantId;
    }

    public String getBankId(){
        return bankId;
    }
    public void setBankId(String bankId){
        this.bankId=bankId;
    }

    public String getGatewayId(){
        return gatewayId;
    }
    public void setGatewayId(String gatewayId){
        this.gatewayId=gatewayId;
    }

    public Double getAmount(){
        return amount;
    }
    public void setAmount(Double amount){
        this.amount=amount;
    }

    public String getLocation(){
        return location;
    }
    public void setLocation(String location){
        this.location=location;
    }

    public String getPaymentMethod(){
        return paymentMethod;
    }
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getResponseCode() {
        return responseCode;
    }
    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }
    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public LocalDateTime getTimestamp(){
        return timestamp;
    }
    public void setTimestamp(LocalDateTime timestamp){
        this.timestamp=timestamp;
    }
}
