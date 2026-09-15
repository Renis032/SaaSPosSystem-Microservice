package com.renko.mapper;

import com.renko.entities.UserEntity;
import com.renko.payload.dto.UserDto;

public class UserMapper
{
    public static UserDto toDto(UserEntity savedUserEntity)
    {
        UserDto userDto = new UserDto();
        userDto.setFromEntity(savedUserEntity);
        return userDto;
    }

    public static UserEntity toEntity(UserDto userDto)
    {
        UserEntity userEntity = new UserEntity();
        userEntity.setFromDto(userDto);
        return userEntity;
    }
}
