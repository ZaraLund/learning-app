package com.example.learningproject.mapper;

import com.example.learningproject.dto.BookingDto;
import com.example.learningproject.entity.Booking;

public final class BookingMapper {

    private BookingMapper() {}

    public static BookingDto toDto(Booking booking) {
        return new BookingDto(
                booking.getId(),
                booking.getRoom().getId(),
                booking.getRoom().getName(),
                booking.getUser().getId(),
                booking.getUser().getName(),
                booking.getStartTime(),
                booking.getEndTime()
        );
    }
}
