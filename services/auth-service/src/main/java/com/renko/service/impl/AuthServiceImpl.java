package com.renko.service.impl;

import com.renko.configuration.JwtProvider;
import com.renko.domain.UserRole;
import com.renko.exceptions.UserException;
import com.renko.mapper.UserMapper;
import com.renko.entities.UserEntity;
import com.renko.payload.dto.AuthRequestDto;
import com.renko.payload.response.AuthResponse;
import com.renko.repository.UserRepository;
import com.renko.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService
{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    public AuthResponse signUp(AuthRequestDto authRequestDto) throws UserException
    {
        if(authRequestDto.getFullName() == null || authRequestDto.getFullName().isBlank())
        {
            throw UserException.withDetail("Full name is required", "fullName", authRequestDto.getFullName());
        }

        if(userRepository.findByEmail(authRequestDto.getEmail()) != null)
        {
            throw UserException.withDetail("Email is already registered", "email", authRequestDto.getEmail());
        }

        UserRole requestedRole = authRequestDto.getRole();
        if(requestedRole == null || requestedRole == UserRole.USER)
        {
            requestedRole = UserRole.OWNER;
        }
        if(requestedRole != UserRole.OWNER)
        {
            throw UserException.withDetail(
                    "Public signup only allows OWNER. Create cashiers and managers via the employees API.",
                    "role",
                    requestedRole
            );
        }

        UserEntity newUserEntity = new UserEntity();
        newUserEntity.setEmail(authRequestDto.getEmail());
        newUserEntity.setPassword(passwordEncoder.encode(authRequestDto.getPassword()));
        newUserEntity.setRole(requestedRole);
        newUserEntity.setPhoneNumber(authRequestDto.getPhoneNumber());
        newUserEntity.setFullName(authRequestDto.getFullName());
        newUserEntity.setCreatedAt(LocalDateTime.now());
        newUserEntity.setUpdatedAt(LocalDateTime.now());
        newUserEntity.setLastLoginAt(LocalDateTime.now());

        UserEntity savedUserEntity = userRepository.save(newUserEntity);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(savedUserEntity.getEmail());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtProvider.generateToken(authentication, savedUserEntity.getId(), savedUserEntity.getStoreId());

        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(jwt);
        authResponse.setMessage("Registered successfully!");
        authResponse.setUser(UserMapper.toDto(savedUserEntity));
        return authResponse;
    }

    @Override
    public AuthResponse login(AuthRequestDto authRequestDto) throws UserException
    {
        String email = authRequestDto.getEmail();
        String password = authRequestDto.getPassword();

        Authentication authentication = authenticate(email, password);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserEntity userEntity = userRepository.findByEmail(email);
        userEntity.setLastLoginAt(LocalDateTime.now());
        userRepository.save(userEntity);

        String jwt = jwtProvider.generateToken(authentication, userEntity.getId(), userEntity.getStoreId());

        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(jwt);
        authResponse.setMessage("Login successfully!");
        authResponse.setUser(UserMapper.toDto(userEntity));
        return authResponse;
    }

    private Authentication authenticate(String email, String password) throws UserException
    {
        try
        {
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
            if(!passwordEncoder.matches(password, userDetails.getPassword()))
            {
                throw UserException.withDetail("Wrong password for the given email", "email", email);
            }
            return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        }
        catch(UsernameNotFoundException e)
        {
            throw UserException.withDetail("No user exists with the given email", "email", email);
        }
    }
}
