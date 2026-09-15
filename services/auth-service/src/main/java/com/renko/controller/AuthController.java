package com.renko.controller;

import jakarta.validation.Valid;
import com.renko.exceptions.UserException;
import com.renko.payload.dto.AuthRequestDto;
import com.renko.payload.response.AuthResponse;
import com.renko.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController
{
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signUp(@Valid @RequestBody AuthRequestDto authRequestDto) throws UserException
    {
        return ResponseEntity.ok(authService.signUp(authRequestDto));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequestDto authRequestDto) throws UserException
    {
        return ResponseEntity.ok(authService.login(authRequestDto));
    }
}
