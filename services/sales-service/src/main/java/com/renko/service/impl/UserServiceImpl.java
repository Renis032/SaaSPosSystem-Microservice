package com.renko.service.impl;

import com.renko.configuration.JwtProvider;
import com.renko.domain.UserRole;
import com.renko.exceptions.UserException;
import com.renko.payload.dto.UserDto;
import com.renko.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService
{
    private final JwtProvider jwtProvider;

    @Override
    public UserDto getUserFromJwtToken(String token) throws UserException
    {
        UserDto dto = new UserDto();
        dto.setEmail(jwtProvider.getEmailFromToken(token));
        dto.setId(jwtProvider.getUserIdFromToken(token));
        dto.setStoreId(jwtProvider.getStoreIdFromToken(token));
        String authorities = jwtProvider.getAuthoritiesFromToken(token);
        if(authorities != null && authorities.contains("ROLE_"))
        {
            for(String part : authorities.split(","))
            {
                String p = part.trim();
                if(p.startsWith("ROLE_"))
                {
                    try
                    {
                        dto.setRole(UserRole.valueOf(p.substring(5)));
                        break;
                    }
                    catch(IllegalArgumentException ignored)
                    {
                    }
                }
            }
        }
        if(dto.getEmail() == null || "null".equals(dto.getEmail()))
        {
            throw UserException.withDetail("Invalid JWT: missing email claim", "token", "present");
        }
        return dto;
    }

    @Override
    public UserDto getCurrentUser() throws UserException
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth == null || !auth.isAuthenticated())
        {
            throw new UserException("No authenticated user in security context");
        }
        if(auth.getDetails() instanceof UserDto details)
        {
            return details;
        }
        UserDto dto = new UserDto();
        dto.setEmail(auth.getName());
        return dto;
    }

    @Override
    public UserDto getUserByEmail(String email) throws UserException
    {
        throw new UserException("User lookup by email is only available on auth-service");
    }

    @Override
    public UserDto getUserById(Long id) throws Exception
    {
        throw new UserException("User lookup by id is only available on auth-service; use auth client");
    }

    @Override
    public List<UserDto> getAllUsers()
    {
        return Collections.emptyList();
    }

    @Override
    public UserDto getAdminUser() throws UserException
    {
        throw new UserException("Admin user lookup is only available on auth-service");
    }

    @Override
    public void deleteById(Long id) throws UserException
    {
        throw new UserException("User deletion is only available on auth-service");
    }

    @Override
    public void deleteAllUsers() throws UserException
    {
        throw new UserException("User deletion is only available on auth-service");
    }
}
