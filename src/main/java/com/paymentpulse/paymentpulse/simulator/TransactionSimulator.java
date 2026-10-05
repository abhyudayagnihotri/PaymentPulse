package com.paymentpulse.paymentpulse.simulator;

import com.paymentpulse.paymentpulse.model.Transaction;
import com.paymentpulse.paymentpulse.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TransactionSimulator {

    private final TransactionRepository repository;
    private final Random random= new Random();

    private final String[] gateways={"G01","G02","G03"};

    private final String[] banks={"B12","B15","B18","B21"};

    private final String[] merchants={"M1001","M1002","M1003","M1004","M1005"};

    private final String[] locations = {"Delhi", "Mumbai", "Bangalore", "Hyderabad", "Chennai", "Pune"};

    private final String[] paymentMethods = {"CARD", "UPI", "NET_BANKING"};

    private final String[] failureCodes = {"05","51","91"};

    public TransactionSimulator(TransactionRepository repository){
        this.repository=repository;
    }

    public Transaction generateTransaction(){
        Transaction transaction = new Transaction();

        transaction.setMerchantId(randomElement(merchants));
        transaction.setBankId(randomElement(banks));
        transaction.setGatewayId(randomElement(gateways));
        transaction.setAmount(randomAmount());
        transaction.setLocation(randomElement(locations));
        transaction.setPaymentMethod(randomElement(paymentMethods));
        /*
         * 90% success
         * 10% failure
         */
        boolean success= random.nextDouble() < 0.90;

        if(success) {
            transaction.setStatus("SUCCESS");
            transaction.setResponseCode("00");
            transaction.setLatencyMs(ThreadLocalRandom.current().nextInt(100,400));
        }
        else {
            transaction.setStatus("FAILED");

            transaction.setResponseCode(randomElement(failureCodes));

            transaction.setLatencyMs(ThreadLocalRandom.current().nextInt(1000,3000));
        }
        transaction.setTimestamp(
                LocalDateTime.now()
        );
        return repository.save(transaction);
    }

    public List<Transaction> generateBatch(int count){

        if(count <=0) {
            throw new IllegalArgumentException("count must be greater than 0");
        }

        if (count > 100_000)
            throw new IllegalArgumentException("Maximum batch size is 100000");

        List<Transaction> transactions = new ArrayList<>(count);

        /*
         * Generate transaction over the previous hour.
         * this is important because we want time-distributed data rather than every
         * transaction having the exact same timestamp
         */

        LocalDateTime end = LocalDateTime.now();

        LocalDateTime start = end.minusHours(1);

        long totalSeconds = java.time.Duration.between(start,end).getSeconds();

        for(int i=0; i<count; i++){
            Transaction transaction = createTransaction();

            long randomOffset = ThreadLocalRandom.current().nextLong(totalSeconds +1 );

            transaction.setTimestamp(start.plusSeconds(randomOffset));

            transactions.add(transaction);
        }
        return repository.saveAll(transactions);
    }

    private Transaction createTransaction(){
        Transaction transaction = new Transaction();

        transaction.setMerchantId(randomElement(merchants));

        transaction.setBankId(randomElement(banks));

        transaction.setGatewayId(randomElement(gateways));

        transaction.setAmount(randomAmount());

        transaction.setLocation(randomElement(locations));

        transaction.setPaymentMethod(randomElement(paymentMethods));

        boolean success = ThreadLocalRandom.current().nextDouble() < 0.90;

        if(success) {
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

    private String randomElement(String[] array) {

        return array[
                random.nextInt(array.length)
                ];
    }

    private double randomAmount() {

        return Math.round(
                (100 + random.nextDouble() * 49900) * 100
        ) / 100.0;
    }

    public List<Transaction> generateGatewayIncident( String gatewayId, int count){
        if(gatewayId == null || gatewayId.isBlank()){
            throw new IllegalArgumentException("Gateway ID must not be empty");
        }
        if(count <= 0 || count > 100000)
            throw new IllegalArgumentException("Count must be between 1 and 100000");

        List<Transaction> transactions = new ArrayList<>();

        for(int i=0; i< count ; i++){

            Transaction transaction = createTransaction();

            transaction.setGatewayId(gatewayId);

            //simulated gateway incident
            boolean failed = random.nextDouble() < 0.40;

            if(failed) {
                transaction.setStatus("FAILED");

                String[] incidentCodes={"05","51","91"};

                transaction.setResponseCode(randomElement(incidentCodes));

                //severe latency during incident
                transaction.setLatencyMs(3000 + random.nextInt(2500));
            }
            else{
                transaction.setStatus("SUCCESS");
                transaction.setResponseCode("00");
            }

            // important:
            // put incident transactions in the current window
            transaction.setTimestamp(LocalDateTime.now().minusSeconds(random.nextInt(540)));
            transactions.add(transaction);
        }
        return repository.saveAll(transactions);
    }
}
