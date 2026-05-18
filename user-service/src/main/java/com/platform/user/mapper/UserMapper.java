package com.platform.user.mapper;

import com.platform.user.dto.UserRequestDto;
import com.platform.user.dto.UserResponseDto;
import com.platform.user.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserEntity toEntity(UserRequestDto dto) {
        UserEntity entity = new UserEntity();
        entity.setName(dto.name());
        entity.setEmail(dto.email());
        return entity;
    }

    public UserResponseDto toDto(UserEntity entity) {
        return new UserResponseDto(entity.getId(), entity.getName(), entity.getEmail());
    }
}
