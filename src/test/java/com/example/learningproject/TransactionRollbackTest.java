package com.example.learningproject;

import com.example.learningproject.dto.CreateBookingRequest;
import com.example.learningproject.entity.Booking;
import com.example.learningproject.repository.BookingRepository;
import com.example.learningproject.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
class TransactionRollbackTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    BookingService bookingService;

    @MockitoSpyBean
    BookingRepository bookingRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void saveShouldBeRolledBackWhenExceptionOccursAfterSave() {
        // Stubbning: spara via JdbcTemplate (deltar i aktiv transaktion om en finns),
        // sedan kasta ett exception – precis som om ett systemfel inträffar direkt efter save().
        doAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            jdbcTemplate.update(
                    "INSERT INTO bookings (room_id, user_id, start_time, end_time) VALUES (?, ?, ?, ?)",
                    booking.getRoom().getId(),
                    booking.getUser().getId(),
                    booking.getStartTime(),
                    booking.getEndTime()
            );
            throw new RuntimeException("Simulated failure after save");
        }).when(bookingRepository).save(any(Booking.class));

        CreateBookingRequest request = new CreateBookingRequest(
                1L,
                1L,
                LocalDateTime.of(2027, 1, 1, 10, 0),
                LocalDateTime.of(2027, 1, 1, 11, 0)
        );

        assertThatThrownBy(() -> bookingService.create(request, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Simulated failure after save");

        reset(bookingRepository); // ta bort stubbingen så count() körs mot riktiga repot

        // Utan @Transactional på BookingService:
        //   jdbcTemplate.update() kör mot en auto-committad connection (ingen aktiv transaktion)
        //   → bokning finns kvar i databasen → count = 1 → TEST FAILAR
        //
        // Med @Transactional på BookingService:
        //   jdbcTemplate.update() deltar i servicens transaktion
        //   → transaktionen rullas tillbaka vid RuntimeException → count = 0 → TEST PASSERAR
        assertThat(bookingRepository.count()).isEqualTo(0);
    }
}
