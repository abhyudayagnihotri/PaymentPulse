package com.paymentpulse.paymentpulse.service;

import com.paymentpulse.paymentpulse.event.TransactionCreatedEvent;
import com.paymentpulse.paymentpulse.model.Transaction;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class TransactionProducerService {

    private static final String TOPIC = "payment-transactions";

    private final KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;

    public TransactionProducerService(KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate){
        this.kafkaTemplate=kafkaTemplate;
    }

    public CompletableFuture<Void> publish(TransactionCreatedEvent event){
        if(event.getEventId()==null || event.getEventId().isBlank()){
            event.setEventId(UUID.randomUUID().toString());
        }

        String key = event.getGatewayId();

        return kafkaTemplate.send(TOPIC,key,event).thenApply(result->{
                    System.out.println("Transaction event published. Event ID: "+event.getEventId()+", Partition: "+ result.getRecordMetadata().partition());
                    return null;
                });
    }
}
