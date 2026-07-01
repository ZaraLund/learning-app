package com.example.learningproject;

import com.example.learningproject.dto.BookingDto;
import com.example.learningproject.dto.CreateBookingRequest;
import com.example.learningproject.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
class ConcurrencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    BookingService bookingService;

    @Test
    void shouldRejectDoubleBookingWhenTwoUsersBookSameRoomAtTheSameTime() throws Exception {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<BookingDto>> futures = new ArrayList<>();

        CreateBookingRequest request = new CreateBookingRequest(
                1L,
                1L,
                LocalDateTime.of(2026, 12, 1, 10, 0),
                LocalDateTime.of(2026, 12, 1, 11, 0)
        );

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startGate.await();
                return bookingService.create(request);
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startGate.countDown();
        executor.shutdown();

        List<BookingDto> accepted = new ArrayList<>();
        List<Exception> rejected = new ArrayList<>();

        for (Future<BookingDto> future : futures) {
            try {
                accepted.add(future.get(5, TimeUnit.SECONDS));
            } catch (ExecutionException e) {
                rejected.add(e);
            }
        }

        // Exakt en bokning ska accepteras, den andra ska avvisas.
        // Testet failar tills overlap-kontroll + locking är implementerat.
        assertThat(accepted).hasSize(1);
        assertThat(rejected).hasSize(1);
    }
}
