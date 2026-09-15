package com.renko.controller;

import com.renko.domain.UserRole;
import com.renko.payload.dto.CreateEmployeeDto;
import com.renko.payload.dto.UserDto;
import com.renko.payload.dto.updates.UserUpdateDto;
import com.renko.payload.response.ApiResponse;
import com.renko.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/employees")
public class EmployeeController
{
    private final EmployeeService employeeService;

    @PostMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('OWNER','STORE_MANAGER','ADMIN')")
    public ResponseEntity<UserDto> createEmployee(@PathVariable Long storeId,
                                                  @RequestBody CreateEmployeeDto createEmployeeDto) throws Exception
    {
        return ResponseEntity.ok(employeeService.createStoreEmployee(createEmployeeDto, storeId));
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllEmployees()
    {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getEmployeeById(@PathVariable Long id) throws Exception
    {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateEmployee(@PathVariable Long id,
                                                  @RequestBody UserUpdateDto userDto) throws Exception
    {
        return ResponseEntity.ok(employeeService.updateStoreEmployee(id, userDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteEmployee(@PathVariable Long id) throws Exception
    {
        employeeService.deleteEmployee(id);

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Employee deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse> deleteAllEmployees() throws Exception
    {
        employeeService.deleteAllEmployees();

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("All employees deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<UserDto>> getEmployeesByStoreId(@PathVariable Long storeId,
                                                               @RequestParam(required = false) UserRole role) throws Exception
    {
        return ResponseEntity.ok(employeeService.findStoreEmployeesByRole(storeId, role));
    }
}
