package com.example.learningproject.mapper;

import com.example.learningproject.dto.RoomDto;
import com.example.learningproject.entity.Room;

public final class RoomMapper {

    private RoomMapper() {}

    public static RoomDto toDto(Room room) {
        return new RoomDto(room.getId(), room.getName());
    }
}
