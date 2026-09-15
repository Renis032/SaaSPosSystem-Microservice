package com.renko.service.impl;

import com.renko.client.AuthServiceClient;
import com.renko.domain.UserRole;
import com.renko.exceptions.ExceptionMessages;
import com.renko.payload.dto.CreateEmployeeDto;
import com.renko.payload.dto.UserDto;
import com.renko.payload.dto.updates.UserUpdateDto;
import com.renko.repository.StoreRepository;
import com.renko.service.EmployeeService;
import com.renko.service.StoreAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService
{
    private final StoreRepository storeRepository;
    private final AuthServiceClient authServiceClient;
    private final StoreAccessService storeAccessService;

    @Override
    public UserDto createStoreEmployee(CreateEmployeeDto employee, Long storeId) throws Exception
    {
        if(storeId == null)
        {
            throw ExceptionMessages.required("storeId", "storeId is required to create an employee. Create a store first.");
        }
        storeAccessService.requireStoreAccess(storeId);
        storeRepository.findById(storeId).orElseThrow(() -> ExceptionMessages.notFound(
                "Store", storeId, "create employee email=" + employee.getEmail()));

        employee.setStoreId(storeId);
        return authServiceClient.createInternalUser(employee);
    }

    @Override
    public UserDto getEmployeeById(Long employeeId) throws Exception
    {
        return authServiceClient.getUser(employeeId);
    }

    @Override
    public List<UserDto> getAllEmployees()
    {
        // Platform-wide list not available without auth admin API; return empty for store service
        return List.of();
    }

    @Override
    public UserDto updateStoreEmployee(Long employeeId, UserUpdateDto employeeDto) throws Exception
    {
        throw new UnsupportedOperationException("Update employee via auth-service");
    }

    @Override
    public void deleteEmployee(Long employeeId) throws Exception
    {
        authServiceClient.deleteUser(employeeId);
    }

    @Override
    public void deleteAllEmployees() throws Exception
    {
        // no-op across services
    }

    @Override
    public List<UserDto> findStoreEmployeesByRole(Long storeId, UserRole role) throws Exception
    {
        storeAccessService.requireStoreAccess(storeId);
        storeRepository.findById(storeId).orElseThrow(() -> ExceptionMessages.notFound(
                "Store", storeId, "list employees"));
        return authServiceClient.listByStore(storeId).stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> u.getRole() != UserRole.ADMIN && u.getRole() != UserRole.OWNER)
                .collect(Collectors.toList());
    }
}
