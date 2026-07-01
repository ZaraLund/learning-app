package com.example.learningproject;

import com.example.learningproject.dto.BookingDto;
import com.example.learningproject.dto.CreateBookingRequest;
import com.example.learningproject.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureRestTestClient
@Testcontainers
class IdempotencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    RestTestClient restTestClient;

    @Autowired
    BookingRepository bookingRepository;

    @Test
    void shouldReturnAlreadyCreatedBookingWhenSameIdempotencyKeyIsUsed() {
        String idempotencyKey = UUID.randomUUID().toString();

        CreateBookingRequest request = new CreateBookingRequest(
                1L,
                1L,
                LocalDateTime.of(2027, 3, 1, 9, 0),
                LocalDateTime.of(2027, 3, 1, 10, 0)
        );

        // Första anropet skapar bokningen
        BookingDto first = restTestClient.post()
                .uri("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BookingDto.class)
                .returnResult()
                .getResponseBody();

        // Andra anropet med samma nyckel ska returnera samma bokning – inte 409, inte en ny bokning
        BookingDto second = restTestClient.post()
                .uri("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BookingDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(bookingRepository.count()).isEqualTo(1);
    }
}
