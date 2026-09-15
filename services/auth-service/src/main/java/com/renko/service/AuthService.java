package com.renko.service;

import com.renko.exceptions.UserException;
import com.renko.payload.dto.AuthRequestDto;
import com.renko.payload.dto.UserDto;
import com.renko.payload.response.AuthResponse;

public interface AuthService
{
    AuthResponse signUp(AuthRequestDto authRequestDto) throws UserException;
    AuthResponse login(AuthRequestDto authRequestDto) throws UserException;
}
