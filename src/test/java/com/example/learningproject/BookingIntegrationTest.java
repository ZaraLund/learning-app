package com.example.learningproject;

import com.example.learningproject.dto.BookingDto;
import com.example.learningproject.dto.CreateBookingRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureRestTestClient
@Testcontainers
class BookingIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    RestTestClient restTestClient;

    @Test
    void shouldCreateBooking() {
        CreateBookingRequest request = new CreateBookingRequest(
                1L,
                1L,
                LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 1, 10, 0)
        );

        restTestClient.post()
                .uri("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BookingDto.class)
                .value(body -> {
                    assertThat(body).isNotNull();
                    assertThat(body.roomId()).isEqualTo(1L);
                    assertThat(body.userId()).isEqualTo(1L);
                    assertThat(body.startTime()).isEqualTo(LocalDateTime.of(2026, 8, 1, 9, 0));
                });
    }

    @Test
    void shouldFetchAllBookings() {
        restTestClient.get()
                .uri("/bookings")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<BookingDto>>() {})
                .value(bookings -> assertThat(bookings).isNotNull());
    }

    @Test
    void shouldDeleteBooking() {
        CreateBookingRequest request = new CreateBookingRequest(
                1L,
                1L,
                LocalDateTime.of(2026, 9, 1, 14, 0),
                LocalDateTime.of(2026, 9, 1, 15, 0)
        );

        BookingDto created = restTestClient.post()
                .uri("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectBody(BookingDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();
        Long bookingId = created.id();

        restTestClient.delete()
                .uri("/bookings/{id}", bookingId)
                .exchange()
                .expectStatus().isNoContent();

        List<BookingDto> remaining = restTestClient.get()
                .uri("/bookings")
                .exchange()
                .returnResult(new ParameterizedTypeReference<List<BookingDto>>() {})
                .getResponseBody();

        assertThat(remaining).noneMatch(b -> b.id().equals(bookingId));
    }
}
