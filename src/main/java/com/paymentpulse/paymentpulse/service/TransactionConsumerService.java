package com.paymentpulse.paymentpulse.service;

import com.paymentpulse.paymentpulse.event.TransactionCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TransactionConsumerService{

    private final TransactionPersistenceService persistenceService;

    public TransactionConsumerService(TransactionPersistenceService persistenceService){
        this.persistenceService=persistenceService;
    }

    @KafkaListener(
            topics = "payment-transactions",
            groupId = "paymentpulse-group"
    )
    public void consume(TransactionCreatedEvent event){
        System.out.println("KAFKA EVENT RECEIVED: "+ event.getEventId() + " | STATUS: "+event.getStatus());

        persistenceService.persist(event);
    }
}