package com.example.learningproject;

import com.example.learningproject.dto.CreateBookingRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureRestTestClient
@Testcontainers
class ConcurrentBookingHttpTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    RestTestClient restTestClient;

    @Test
    void shouldReturnProperErrorNotServerErrorWhenTwoUsersBookSameRoomConcurrently() throws Exception {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<HttpStatusCode>> futures = new ArrayList<>();

        CreateBookingRequest request = new CreateBookingRequest(
                1L,
                1L,
                LocalDateTime.of(2027, 6, 1, 14, 0),
                LocalDateTime.of(2027, 6, 1, 15, 0)
        );

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startGate.await();
                return restTestClient.post()
                        .uri("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .exchange()
                        .returnResult()
                        .getStatus();
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startGate.countDown();
        executor.shutdown();

        List<HttpStatusCode> statuses = new ArrayList<>();
        for (Future<HttpStatusCode> future : futures) {
            statuses.add(future.get(5, TimeUnit.SECONDS));
        }

        // En bokning ska lyckas, den andra ska avvisas med ett klientfel – inte 500.
        // Testet failar tills ett @ExceptionHandler hanterar DataIntegrityViolationException.
        assertThat(statuses).anySatisfy(status -> assertThat(status.value()).isEqualTo(201));
        assertThat(statuses).noneSatisfy(status -> assertThat(status.is5xxServerError()).isTrue());
    }
}
