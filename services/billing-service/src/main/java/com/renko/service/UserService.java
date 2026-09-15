package com.renko.service;

import com.renko.exceptions.UserException;
import com.renko.payload.dto.UserDto;

import java.util.List;

public interface UserService
{
    UserDto getUserFromJwtToken(String token) throws UserException;
    UserDto getCurrentUser() throws UserException;
    UserDto getUserByEmail(String email) throws UserException;
    UserDto getUserById(Long id) throws Exception;
    List<UserDto> getAllUsers();
    UserDto getAdminUser() throws UserException;

    void deleteById(Long id) throws UserException;
    void deleteAllUsers() throws UserException;
}
