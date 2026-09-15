package com.renko.payload.dto.updates;

import com.renko.domain.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateDto
{
    private String fullName;
    private String email;
    private String password;

    private String phoneNumber;

    private UserRole role;

    private Long storeId;
}
