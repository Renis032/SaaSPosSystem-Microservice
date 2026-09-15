package com.renko.payload.dto;

import com.renko.domain.UserRole;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDto
{
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private UserRole role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private Long storeId;
}
