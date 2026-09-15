package com.renko.service.impl;

import com.renko.entities.CustomerEntity;
import com.renko.exceptions.ExceptionMessages;
import com.renko.payload.dto.PageResponse;
import com.renko.payload.dto.UserDto;
import com.renko.repository.CustomerRepository;
import com.renko.service.CustomerService;
import com.renko.service.StoreAccessService;
import com.renko.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService
{
    private final CustomerRepository customerRepository;
    private final UserService userService;
    private final StoreAccessService storeAccessService;

    @Override
    public CustomerEntity createCustomer(CustomerEntity customerEntity) throws Exception
    {
        Long storeId = resolveStoreIdForCreate(customerEntity);
        customerEntity.setStoreId(storeId);
        return customerRepository.save(customerEntity);
    }

    @Override
    public CustomerEntity updateCustomer(Long id, CustomerEntity customerEntity) throws Exception
    {
        CustomerEntity customer = customerRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Customer", id, "update"));
        if(customerEntity.getFullName() != null) customer.setFullName(customerEntity.getFullName());
        if(customerEntity.getEmail() != null) customer.setEmail(customerEntity.getEmail());
        if(customerEntity.getPhone() != null) customer.setPhone(customerEntity.getPhone());
        if(customerEntity.getStoreId() != null)
        {
            storeAccessService.requireStoreAccess(customerEntity.getStoreId());
            customer.setStoreId(customerEntity.getStoreId());
        }
        return customerRepository.save(customer);
    }

    @Override
    public void deleteCustomer(Long id) throws Exception
    {
        CustomerEntity customer = customerRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Customer", id, "delete"));
        customerRepository.delete(customer);
    }

    @Override
    public CustomerEntity getCustomer(Long id) throws Exception
    {
        return customerRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Customer", id));
    }

    @Override
    public List<CustomerEntity> getAllCustomers()
    {
        return customerRepository.findAll();
    }

    @Override
    public List<CustomerEntity> getCustomersByStoreEntity_Id(Long id) throws Exception
    {
        storeAccessService.requireStoreAccess(id);
        return customerRepository.findByStoreId(id);
    }

    @Override
    public PageResponse<CustomerEntity> getCustomersByStorePaged(Long storeId, int page, int size, String q) throws Exception
    {
        storeAccessService.requireStoreAccess(storeId);
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        String query = q == null || q.isBlank() ? null : q.trim();
        Page<CustomerEntity> result = customerRepository.searchByStore(
                storeId, query, PageRequest.of(safePage, safeSize, Sort.by("fullName")));
        return PageResponse.of(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Override
    public List<CustomerEntity> searchCustomer(String keyword)
    {
        if(keyword == null || keyword.isBlank()) return List.of();
        Map<Long, CustomerEntity> unique = new LinkedHashMap<>();
        for(CustomerEntity c : customerRepository.findByPhoneContainingIgnoreCase(keyword)) unique.put(c.getId(), c);
        for(CustomerEntity c : customerRepository.findByFullNameContainingIgnoreCase(keyword)) unique.put(c.getId(), c);
        return new ArrayList<>(unique.values());
    }

    @Override
    public void deleteAllCustomers()
    {
        customerRepository.deleteAll();
    }

    private Long resolveStoreIdForCreate(CustomerEntity customerEntity) throws Exception
    {
        if(customerEntity.getStoreId() != null)
        {
            storeAccessService.requireStoreAccess(customerEntity.getStoreId());
            return customerEntity.getStoreId();
        }
        UserDto user = userService.getCurrentUser();
        if(user.getStoreId() == null)
        {
            throw ExceptionMessages.required("storeId", "storeId is required to create a customer");
        }
        storeAccessService.requireStoreAccess(user.getStoreId());
        return user.getStoreId();
    }
}
