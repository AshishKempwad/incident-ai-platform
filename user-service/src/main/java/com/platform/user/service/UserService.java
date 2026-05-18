package com.platform.user.service;

import com.platform.user.dto.UserRequestDto;
import com.platform.user.dto.UserResponseDto;
import com.platform.user.entity.UserEntity;
import com.platform.user.exception.ConflictException;
import com.platform.user.exception.ResourceNotFoundException;
import com.platform.user.mapper.UserMapper;
import com.platform.user.repository.UserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository repository;
    private final UserMapper mapper;

    public UserService(UserRepository repository, UserMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public UserResponseDto create(UserRequestDto requestDto) {
        repository.findByEmail(requestDto.email()).ifPresent(u -> {
            throw new ConflictException("email already exists");
        });
        UserEntity saved = repository.save(mapper.toEntity(requestDto));
        log.info("event=user_created userId={}", saved.getId());
        return mapper.toDto(saved);
    }

    public List<UserResponseDto> list() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    public UserResponseDto getById(Long id) {
        return mapper.toDto(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("user not found: " + id)));
    }
}
