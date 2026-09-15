package com.renko.controller;

import com.renko.exceptions.UserException;
import com.renko.payload.dto.CreateEmployeeDto;
import com.renko.payload.dto.UserDto;
import com.renko.payload.response.ApiResponse;
import com.renko.service.UserService;
import com.renko.service.impl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController
{
    private final UserService userService;
    private final UserServiceImpl userServiceImpl;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    public ResponseEntity<UserDto> getUserProfile(@RequestHeader("Authorization") String jwt) throws UserException
    {
        return ResponseEntity.ok(userService.getUserFromJwtToken(jwt));
    }

    @GetMapping("/admin")
    public ResponseEntity<UserDto> getAdminUser() throws UserException
    {
        return ResponseEntity.ok(userService.getAdminUser());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) throws Exception
    {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping("/internal")
    public ResponseEntity<UserDto> createInternal(@RequestBody CreateEmployeeDto body) throws UserException
    {
        UserDto dto = new UserDto();
        dto.setEmail(body.getEmail());
        dto.setFullName(body.getFullName());
        dto.setPhoneNumber(body.getPhoneNumber());
        dto.setRole(body.getRole());
        dto.setStoreId(body.getStoreId());
        return ResponseEntity.ok(userServiceImpl.createInternalUser(dto, body.getPassword(), passwordEncoder));
    }

    @PatchMapping("/{id}/store")
    public ResponseEntity<UserDto> linkStore(@PathVariable Long id, @RequestBody Map<String, Long> body) throws UserException
    {
        Long storeId = body != null ? body.get("storeId") : null;
        if(storeId == null)
        {
            throw UserException.withDetail("storeId is required", "storeId", null);
        }
        return ResponseEntity.ok(userServiceImpl.linkStore(id, storeId));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<UserDto>> byStore(@PathVariable Long storeId)
    {
        return ResponseEntity.ok(userServiceImpl.findByStoreId(storeId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteUserById(@PathVariable Long id) throws UserException
    {
        userService.deleteById(id);
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("User deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse> deleteAllUsers() throws UserException
    {
        userService.deleteAllUsers();
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("All deletable users removed successfully");
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers()
    {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}
