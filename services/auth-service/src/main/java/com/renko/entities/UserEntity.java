package com.renko.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.renko.domain.UserRole;
import com.renko.payload.dto.UserDto;
import com.renko.payload.dto.updates.UserUpdateDto;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Table(name = "user")
public class UserEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    @Email(message = "Email should be valid")
    private String email;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    private String phoneNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private UserRole role;

    @Column(name = "store_id")
    private Long storeId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    public void setFromDto(UserDto userDto)
    {
        id = userDto.getId();
        fullName = userDto.getFullName();
        email = userDto.getEmail();
        phoneNumber = userDto.getPhoneNumber();
        role = userDto.getRole();
        createdAt = userDto.getCreatedAt();
        updatedAt = userDto.getUpdatedAt();
        lastLoginAt = userDto.getLastLoginAt();
        storeId = userDto.getStoreId();
    }

    public void updateFrom(UserUpdateDto dto)
    {
        if(dto.getEmail() != null) this.setEmail(dto.getEmail());
        if(dto.getFullName() != null) this.setFullName(dto.getFullName());
        if(dto.getPhoneNumber() != null) this.setPhoneNumber(dto.getPhoneNumber());
        if(dto.getRole() != null) this.setRole(dto.getRole());
        if(dto.getStoreId() != null) this.setStoreId(dto.getStoreId());
    }
}
