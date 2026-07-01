package com.example.learningproject.dto;

import java.time.LocalDateTime;

public record BookingDto(
        Long id,
        Long roomId,
        String roomName,
        Long userId,
        String userName,
        LocalDateTime startTime,
        LocalDateTime endTime
) {}
