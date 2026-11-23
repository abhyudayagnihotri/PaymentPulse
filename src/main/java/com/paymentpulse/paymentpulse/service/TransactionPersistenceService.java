package com.paymentpulse.paymentpulse.service;

import com.paymentpulse.paymentpulse.event.TransactionCreatedEvent;
import com.paymentpulse.paymentpulse.model.ProcessedEvent;
import com.paymentpulse.paymentpulse.model.Transaction;
import com.paymentpulse.paymentpulse.repository.ProcessedEventRepository;
import com.paymentpulse.paymentpulse.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TransactionPersistenceService {
    private final TransactionRepository transactionRepository;
    private final ProcessedEventRepository processedEventRepository;

    public TransactionPersistenceService(TransactionRepository transactionRepository, ProcessedEventRepository processedEventRepository){
        this.transactionRepository=transactionRepository;
        this.processedEventRepository=processedEventRepository;
    }

    @Transactional
    public boolean persist(TransactionCreatedEvent event){

        if(event.getEventId() == null || event.getEventId().isBlank()){
            throw new IllegalArgumentException("Event ID cannot be empty");
        }

        int claimed = processedEventRepository.claimEvent(event.getEventId(),LocalDateTime.now());

        if(claimed == 0) {
            System.out.println("Duplicate Event Skipped: "+event.getEventId());
            return false;
        }

        Transaction transaction = new Transaction();

        transaction.setMerchantId(event.getMerchantId());
        transaction.setBankId(event.getBankId());
        transaction.setGatewayId(event.getGatewayId());
        transaction.setAmount(event.getAmount());
        transaction.setLocation(event.getLocation());
        transaction.setPaymentMethod(event.getPaymentMethod());
        transaction.setStatus(event.getStatus());
        transaction.setResponseCode(event.getResponseCode());
        transaction.setLatencyMs(event.getLatencyMs());
        transaction.setTimestamp(event.getTimestamp());

        transactionRepository.save(transaction);

        System.out.println("Transaction persisted successfully: " + event.getEventId());

        return true;
    }
}
