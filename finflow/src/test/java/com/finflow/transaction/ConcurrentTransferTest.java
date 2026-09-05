package com.finflow.transaction;

import com.finflow.entity.User;
import com.finflow.entity.UserRole;
import com.finflow.entity.UserStatus;
import com.finflow.repository.UserRepository;
import com.finflow.transaction.dto.TransferRequest;
import com.finflow.wallet.Wallet;
import com.finflow.wallet.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConcurrentTransferTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private Wallet senderWallet;
    private Wallet receiverWallet;

    private static final String SENDER_EMAIL = "concurrent-sender@test.com";

    private static final String RECEIVER_EMAIL = "concurrent-receiver@test.com";

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();

        User sender = new User();
        sender.setName("Sender Test");
        sender.setPassword("password123");
        sender.setEmail(SENDER_EMAIL);
        sender.setRole(UserRole.CUSTOMER);
        sender.setStatus(UserStatus.ACTIVE);
        sender = userRepository.save(sender);

        Wallet senderWallet = new Wallet();
        senderWallet.setUser(sender);
        senderWallet.setBalance(new BigDecimal("10000.00"));
        senderWallet.setCurrency("INR");

        User receiver = new User();
        receiver.setName("Receiver Test");
        receiver.setPassword("password123");
        receiver.setEmail(RECEIVER_EMAIL);
        receiver.setRole(UserRole.CUSTOMER);
        receiver.setStatus(UserStatus.ACTIVE);
        receiver = userRepository.save(receiver);

        Wallet receiverWallet = new Wallet();
        receiverWallet.setUser(receiver);
        receiverWallet.setBalance(new BigDecimal("0.00"));
        receiverWallet.setCurrency("INR");

        this.senderWallet = walletRepository.save(senderWallet);
        this.receiverWallet = walletRepository.save(receiverWallet);
    }

    @Test
    void shouldPreventNegativeBalanceDuringConcurrentTransfers()
            throws Exception {

        int numberOfRequests = 15;

        BigDecimal transferAmount =
                new BigDecimal("1000.00");

        ExecutorService executorService =
                Executors.newFixedThreadPool(
                        numberOfRequests
                );

        CountDownLatch startLatch =
                new CountDownLatch(1);

        CountDownLatch completionLatch =
                new CountDownLatch(
                        numberOfRequests
                );

        List<Future<TransactionService.TransferResult>>
                futures = new ArrayList<>();


        for (int i = 0; i < numberOfRequests; i++) {

            int requestNumber = i;
            Future<TransactionService.TransferResult>
                    future = executorService.submit(() -> {

                try {

                    startLatch.await();

                    TransferRequest request =
                            new TransferRequest(
                                    receiverWallet.getId(),
                                    transferAmount,
                                    "concurrent-ref-" + requestNumber,
                                    "Concurrent transfer test"
                            );

                    return transactionService.transfer(
                            SENDER_EMAIL,
                            request
                    );

                } finally {
                    completionLatch.countDown();
                }
            });

            futures.add(future);
        }

        startLatch.countDown();

        boolean completed =
                completionLatch.await(
                        30,
                        TimeUnit.SECONDS
                );

        assertTrue(
                completed,
                "Concurrent transfers did not complete in time"
        );

        int successfulTransfers = 0;
        int failedTransfers = 0;

        for (Future<TransactionService.TransferResult>
                future : futures) {

            try {
                TransactionService.TransferResult result =
                        future.get();

                assertNotNull(result);
                successfulTransfers++;

            } catch (ExecutionException exception) {
                failedTransfers++;
            }
        }

        executorService.shutdown();
        Wallet finalSenderWallet =
                walletRepository
                        .findById(senderWallet.getId())
                        .orElseThrow();

        Wallet finalReceiverWallet =
                walletRepository
                        .findById(receiverWallet.getId())
                        .orElseThrow();

        // -----------------------------------------------------
        // Expected:
        //
        // Initial sender balance = ₹10,000
        // Each transfer = ₹1,000
        // 15 requests
        //
        // Maximum successful transfers = 10
        // -----------------------------------------------------

        assertEquals(
                10,
                successfulTransfers,
                "Exactly 10 transfers should succeed"
        );

        assertEquals(
                5,
                failedTransfers,
                "Exactly 5 transfers should fail"
        );

        assertTrue(
                finalSenderWallet
                        .getBalance()
                        .compareTo(BigDecimal.ZERO) >= 0,
                "Sender balance must never be negative"
        );

        assertEquals(
                0,
                finalSenderWallet
                        .getBalance()
                        .compareTo(
                                new BigDecimal("0.00")
                        )
        );


        assertEquals(
                0,
                finalReceiverWallet
                        .getBalance()
                        .compareTo(
                                new BigDecimal("10000.00")
                        )
        );

        long transactionCount =
                transactionRepository.count();

        assertEquals(
                10,
                transactionCount,
                "Only successful transfers should create transactions"
        );
    }

    @Test
    void shouldPreventDeadlockForOppositeDirectionTransfers()
            throws Exception {

        receiverWallet.setBalance(
                new BigDecimal("10000.00")
        );

        walletRepository.save(receiverWallet);

        int numberOfRequests = 20;

        BigDecimal transferAmount =
                new BigDecimal("100.00");

        ExecutorService executorService =
                Executors.newFixedThreadPool(
                        numberOfRequests
                );

        CountDownLatch startLatch =
                new CountDownLatch(1);

        CountDownLatch completionLatch =
                new CountDownLatch(
                        numberOfRequests
                );

        List<Future<TransactionService.TransferResult>>
                futures = new ArrayList<>();


        for (int i = 0; i < numberOfRequests; i++) {

            int requestNumber = i;
            Future<TransactionService.TransferResult>
                    future = executorService.submit(() -> {
                try {

                    startLatch.await();
                    boolean senderToReceiver =
                            requestNumber % 2 == 0;

                    String senderEmail;
                    UUID receiverId;

                    if (senderToReceiver) {
                        senderEmail = SENDER_EMAIL;
                        receiverId = receiverWallet.getId();
                    } else {
                        senderEmail = RECEIVER_EMAIL;
                        receiverId = senderWallet.getId();
                    }

                    TransferRequest request =
                            new TransferRequest(
                                    receiverId,
                                    transferAmount,
                                    "deadlock-ref-" + requestNumber,
                                    "Deadlock prevention test"
                            );

                    return transactionService.transfer(
                            senderEmail,
                            request
                    );

                } finally {
                    completionLatch.countDown();
                }
            });

            futures.add(future);
        }

        startLatch.countDown();

        boolean completed =
                completionLatch.await(
                        30,
                        TimeUnit.SECONDS
                );

        assertTrue(
                completed,
                "Possible deadlock detected: transfers did not complete"
        );


        int completedRequests = 0;
        for (Future<TransactionService.TransferResult>
                future : futures) {
            try {

                TransactionService.TransferResult result =
                        future.get(
                                5,
                                TimeUnit.SECONDS
                        );

                assertNotNull(result);
                completedRequests++;

            } catch (ExecutionException exception) {
                completedRequests++;

            }
        }

        executorService.shutdown();

        assertEquals(
                numberOfRequests,
                completedRequests
        );

        Wallet finalSenderWallet =
                walletRepository
                        .findById(senderWallet.getId())
                        .orElseThrow();

        Wallet finalReceiverWallet =
                walletRepository
                        .findById(receiverWallet.getId())
                        .orElseThrow();

        assertTrue(
                finalSenderWallet
                        .getBalance()
                        .compareTo(BigDecimal.ZERO) >= 0,
                "Sender balance must never be negative"
        );

        assertTrue(
                finalReceiverWallet
                        .getBalance()
                        .compareTo(BigDecimal.ZERO) >= 0,
                "Receiver balance must never be negative"
        );

        // -----------------------------------------------------
        // Money conservation check
        //
        // Initial total:
        //
        // ₹10,000 + ₹10,000 = ₹20,000
        //
        // Final total must still be:
        //
        // ₹20,000
        // -----------------------------------------------------

        BigDecimal finalTotal =
                finalSenderWallet
                        .getBalance()
                        .add(
                                finalReceiverWallet
                                        .getBalance()
                        );

        assertEquals(
                0,
                finalTotal.compareTo(
                        new BigDecimal("20000.00")
                ),
                "Money must not be created or lost"
        );
    }
}

