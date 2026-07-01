package com.example.learningproject.dto;

import java.time.LocalDateTime;

public record CreateBookingRequest(
        Long roomId,
        Long userId,
        LocalDateTime startTime,
        LocalDateTime endTime
) {}
