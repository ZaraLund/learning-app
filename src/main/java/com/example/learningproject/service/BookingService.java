package com.example.learningproject.service;

import com.example.learningproject.dto.BookingDto;
import com.example.learningproject.dto.CreateBookingRequest;
import com.example.learningproject.entity.Booking;
import com.example.learningproject.entity.Room;
import com.example.learningproject.entity.User;
import com.example.learningproject.mapper.BookingMapper;
import com.example.learningproject.repository.BookingRepository;
import com.example.learningproject.repository.RoomRepository;
import com.example.learningproject.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public BookingService(
            BookingRepository bookingRepository,
            RoomRepository roomRepository,
            UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    public List<BookingDto> findAll() {
        return bookingRepository.findAll().stream()
                .map(BookingMapper::toDto)
                .toList();
    }

    @Transactional
    public BookingDto create(CreateBookingRequest request, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<Booking> existing = bookingRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return BookingMapper.toDto(existing.get());
            }
        }

        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + request.roomId()));

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.userId()));

        Booking booking = new Booking(room, user, request.startTime(), request.endTime());
        booking.setIdempotencyKey(idempotencyKey);

        return BookingMapper.toDto(bookingRepository.save(booking));
    }

    public void delete(Long id) {
        bookingRepository.deleteById(id);
    }
}
