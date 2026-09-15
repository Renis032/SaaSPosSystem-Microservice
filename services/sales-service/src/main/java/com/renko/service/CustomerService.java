package com.renko.service;

import com.renko.entities.CustomerEntity;
import com.renko.payload.dto.PageResponse;

import java.util.List;

public interface CustomerService
{
    CustomerEntity createCustomer(CustomerEntity customerEntity) throws Exception;
    CustomerEntity updateCustomer(Long id, CustomerEntity customerEntity) throws Exception;
    void deleteCustomer(Long id) throws Exception;
    CustomerEntity getCustomer(Long id) throws Exception;
    List<CustomerEntity> getAllCustomers();
    List<CustomerEntity> getCustomersByStoreEntity_Id(Long id) throws Exception;
    PageResponse<CustomerEntity> getCustomersByStorePaged(Long storeId, int page, int size, String q) throws Exception;
    List<CustomerEntity> searchCustomer(String keyword);
    void deleteAllCustomers();
}
