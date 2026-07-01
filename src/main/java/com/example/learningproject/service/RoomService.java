package com.example.learningproject.service;

import com.example.learningproject.dto.RoomDto;
import com.example.learningproject.mapper.RoomMapper;
import com.example.learningproject.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<RoomDto> findAll() {
        return roomRepository.findAll().stream()
                .map(RoomMapper::toDto)
                .toList();
    }
}
