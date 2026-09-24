package com.sehaaz.eventtix.auth.api;

import com.sehaaz.eventtix.auth.api.dto.LoginRequest;
import com.sehaaz.eventtix.auth.api.dto.LoginResponse;
import com.sehaaz.eventtix.auth.api.dto.RegisterRequest;
import com.sehaaz.eventtix.auth.api.dto.UserResponse;
import com.sehaaz.eventtix.auth.domain.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
