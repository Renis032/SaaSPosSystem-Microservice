package com.renko.service.impl;

import com.renko.configuration.JwtProvider;
import com.renko.domain.UserRole;
import com.renko.entities.UserEntity;
import com.renko.exceptions.UserException;
import com.renko.mapper.UserMapper;
import com.renko.payload.dto.UserDto;
import com.renko.repository.UserRepository;
import com.renko.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService
{
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Override
    public UserDto getUserFromJwtToken(String token) throws UserException
    {
        String email = jwtProvider.getEmailFromToken(token);
        UserEntity userEntity = userRepository.findByEmail(email);
        if(userEntity == null)
        {
            throw UserException.withDetail(
                    "JWT is valid but no user matches the token email",
                    "email",
                    email
            );
        }
        return UserMapper.toDto(userEntity);
    }

    @Override
    public UserDto getCurrentUser() throws UserException
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth != null && auth.getDetails() instanceof UserDto details && details.getId() != null)
        {
            return userRepository.findById(details.getId())
                    .map(UserMapper::toDto)
                    .orElse(details);
        }
        String email = auth != null ? auth.getName() : null;
        UserEntity currentUserEntity = userRepository.findByEmail(email);
        if(currentUserEntity == null)
        {
            throw UserException.withDetail(
                    "Authenticated principal does not match any user in the database",
                    "email",
                    email
            );
        }
        return UserMapper.toDto(currentUserEntity);
    }

    @Override
    public UserDto getUserByEmail(String email) throws UserException
    {
        UserEntity userEntity = userRepository.findByEmail(email);
        if(userEntity == null)
        {
            throw UserException.withDetail("User not found with the given email", "email", email);
        }
        return UserMapper.toDto(userEntity);
    }

    @Override
    public UserDto getUserById(Long id) throws Exception
    {
        return UserMapper.toDto(userRepository.findById(id).orElseThrow(() ->
                new Exception("User not found with id: " + id)));
    }

    @Override
    public List<UserDto> getAllUsers()
    {
        return userRepository.findAll().stream().map(UserMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteById(Long id) throws UserException
    {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> UserException.withDetail("User not found; cannot delete", "userId", id));
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public void deleteAllUsers() throws UserException
    {
        userRepository.deleteAll();
    }

    @Override
    public UserDto getAdminUser() throws UserException
    {
        return UserMapper.toDto(userRepository.findByRole(UserRole.ADMIN)
                .orElseThrow(() -> UserException.withDetail("Admin user not found", "role", UserRole.ADMIN)));
    }

    @Transactional
    public UserDto createInternalUser(UserDto dto, String rawPassword, org.springframework.security.crypto.password.PasswordEncoder encoder) throws UserException
    {
        if(userRepository.findByEmail(dto.getEmail()) != null)
        {
            throw UserException.withDetail("Email is already registered", "email", dto.getEmail());
        }
        UserEntity user = new UserEntity();
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setRole(dto.getRole() != null ? dto.getRole() : UserRole.CASHIER);
        user.setStoreId(dto.getStoreId());
        user.setPassword(encoder.encode(rawPassword));
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setUpdatedAt(java.time.LocalDateTime.now());
        return UserMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto linkStore(Long userId, Long storeId) throws UserException
    {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> UserException.withDetail("User not found", "userId", userId));
        user.setStoreId(storeId);
        user.setUpdatedAt(java.time.LocalDateTime.now());
        return UserMapper.toDto(userRepository.save(user));
    }

    public List<UserDto> findByStoreId(Long storeId)
    {
        return userRepository.findByStoreId(storeId).stream().map(UserMapper::toDto).collect(Collectors.toList());
    }
}
