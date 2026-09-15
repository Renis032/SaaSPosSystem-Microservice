package com.renko.service;

import com.renko.domain.UserRole;
import com.renko.payload.dto.CreateEmployeeDto;
import com.renko.payload.dto.UserDto;
import com.renko.payload.dto.updates.UserUpdateDto;

import java.util.List;

public interface EmployeeService
{
    UserDto createStoreEmployee(CreateEmployeeDto employee, Long storeId) throws Exception;
    UserDto getEmployeeById(Long employeeId) throws Exception;
    List<UserDto> getAllEmployees();
    UserDto updateStoreEmployee(Long employeeId, UserUpdateDto employeeDto) throws Exception;
    void deleteEmployee(Long employeeId) throws Exception;
    void deleteAllEmployees() throws Exception;
    List<UserDto> findStoreEmployeesByRole(Long storeId, UserRole role) throws Exception;
}
