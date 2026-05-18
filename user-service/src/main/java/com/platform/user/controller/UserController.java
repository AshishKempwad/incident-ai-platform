package com.platform.user.controller;

import com.platform.user.dto.UserRequestDto;
import com.platform.user.dto.UserResponseDto;
import com.platform.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto create(@Valid @RequestBody UserRequestDto dto) {
        return service.create(dto);
    }

    @GetMapping
    public List<UserResponseDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public UserResponseDto getById(@PathVariable Long id) {
        return service.getById(id);
    }
}
