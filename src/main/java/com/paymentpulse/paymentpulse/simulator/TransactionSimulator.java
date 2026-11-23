package com.paymentpulse.paymentpulse.simulator;

import com.paymentpulse.paymentpulse.event.TransactionCreatedEvent;
import com.paymentpulse.paymentpulse.model.Transaction;
import com.paymentpulse.paymentpulse.service.TransactionProducerService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TransactionSimulator{

    private final TransactionProducerService producer;
    private final Random random=new Random();

    private final String[] gateways = {"G01","G02","G03"};
    private final String[] banks = {"B12","B15","B18","B21"};
    private final String[] merchants = {"M1001","M1002","M1003","M1004","M1005"};
    private final String[] locations = {"Delhi","Mumbai","Bangalore","Hyderabad","Chennai","Pune"};
    private final String[] paymentMethods ={"CARD","UPI","NET_BANKING"};
    private final String[] failureCodes ={"05","51","91"};

    public TransactionSimulator(TransactionProducerService producer) {
        this.producer = producer;
    }

    public Transaction generateTransaction() {

        Transaction transaction = createTransaction();

        transaction.setTimestamp(LocalDateTime.now());

        producer.publish(toEvent(transaction)).join();

        return transaction;
    }

    public List<Transaction> generateBatch(int count) {

        if(count <= 0){
            throw new IllegalArgumentException("Count must be greater than 0");
        }

        if(count > 100_000) {
            throw new IllegalArgumentException("Maximum batch size is 100000");
        }

        List<Transaction> transactions = new ArrayList<>(count);
        List<CompletableFuture<Void>> futures = new ArrayList<>(count);

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusMinutes(70);

        long totalSeconds = Duration.between(start, end).getSeconds();

        for(int i=0; i<count; i++){

            Transaction transaction = createTransaction();

            long randomOffset = ThreadLocalRandom.current().nextLong (totalSeconds + 1);

            transaction.setTimestamp(start.plusSeconds(randomOffset));

            transactions.add(transaction);

            futures.add(producer.publish(toEvent(transaction)));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return transactions;
    }

    public List<Transaction> generateGatewayIncident(String gatewayId, int count){
        if (gatewayId == null || gatewayId.isBlank()) {
            throw new IllegalArgumentException("Gateway ID must not be empty");
        }

        if(count <= 0 || count > 100_000){
            throw new IllegalArgumentException("GatewayId must be between 1 and 100000");
        }

        List<Transaction> transactions = new ArrayList<>(count);
        List<CompletableFuture<Void>> futures = new ArrayList<>(count);

        for(int i=0; i < count; i++) {

            Transaction transaction = createTransaction();

            transaction.setGatewayId(gatewayId);

            boolean failed = random.nextDouble() < 0.40;

            if(failed) {
                transaction.setStatus("FAILED");

                String[] incidentCodes = {"05", "51", "91"};
                transaction.setResponseCode(randomElement(incidentCodes));

                transaction.setLatencyMs(3000 + random.nextInt(2500));
            }

            else {
                transaction.setStatus("SUCCESS");
                transaction.setResponseCode("00");
            }

            transaction.setTimestamp(LocalDateTime.now().minusSeconds(random.nextInt(540)));

            transactions.add(transaction);

            futures.add(producer.publish(toEvent(transaction)));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return transactions;
    }

    private Transaction createTransaction() {

        Transaction transaction = new Transaction();

        transaction.setMerchantId(randomElement(merchants));
        transaction.setBankId(randomElement(banks));
        transaction.setGatewayId(randomElement(gateways));
        transaction.setAmount(randomAmount());
        transaction.setLocation(randomElement(locations));
        transaction.setPaymentMethod(randomElement(paymentMethods));

        boolean success = random.nextDouble() < 0.90;

        if(success){
            transaction.setStatus("SUCCESS");
            transaction.setResponseCode("00");
            transaction.setLatencyMs(ThreadLocalRandom.current().nextInt(100,400));
        }

        else {
            transaction.setStatus("FAILED");
            transaction.setResponseCode(randomElement(failureCodes));
            transaction.setLatencyMs(ThreadLocalRandom.current().nextInt(1000,3000));
        }

        return transaction;
    }

    private TransactionCreatedEvent toEvent(Transaction transaction){

        TransactionCreatedEvent event = new TransactionCreatedEvent();

        event.setMerchantId(transaction.getMerchantId());
        event.setBankId(transaction.getBankId());
        event.setGatewayId(transaction.getGatewayId());
        event.setAmount(transaction.getAmount());
        event.setTimestamp(transaction.getTimestamp());
        event.setLatencyMs(transaction.getLatencyMs());
        event.setResponseCode(transaction.getResponseCode());
        event.setPaymentMethod(transaction.getPaymentMethod());
        event.setLocation(transaction.getLocation());
        event.setStatus(transaction.getStatus());

        return event;
    }

    private String randomElement(String[] array) {
        return array[random.nextInt(array.length)];
    }

    private double randomAmount() {
        return Math.round( (100+ random.nextDouble()*49900) * 100)/100.0;
    }
}