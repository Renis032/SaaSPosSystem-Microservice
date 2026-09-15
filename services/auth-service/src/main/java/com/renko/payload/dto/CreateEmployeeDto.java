package com.renko.payload.dto;

import com.renko.domain.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmployeeDto
{
    private String email;
    private String password;
    private String fullName;
    private String phoneNumber;
    private UserRole role;

    private Long storeId;

}
