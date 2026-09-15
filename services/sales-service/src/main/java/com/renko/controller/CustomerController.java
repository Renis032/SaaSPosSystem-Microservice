package com.renko.controller;

import com.renko.entities.CustomerEntity;
import com.renko.payload.dto.UserDto;
import com.renko.payload.response.ApiResponse;
import com.renko.service.CustomerService;
import com.renko.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class CustomerController
{
    private final CustomerService customerService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<CustomerEntity> createCustomer(@RequestBody CustomerEntity customerEntity) throws Exception
    {
        return ResponseEntity.ok(customerService.createCustomer(customerEntity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerEntity> updateCustomer(@PathVariable Long id,
                                                         @RequestBody CustomerEntity customerEntity) throws Exception
    {
        return ResponseEntity.ok(customerService.updateCustomer(id, customerEntity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteCustomer(@PathVariable Long id) throws Exception
    {
        customerService.deleteCustomer(id);
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Customer deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping
    public ResponseEntity<List<CustomerEntity>> getAllCustomer(@RequestHeader("Authorization") String jwt) throws Exception
    {
        UserDto userDto = userService.getUserFromJwtToken(jwt);
        if(userDto.getStoreId() != null)
        {
            return ResponseEntity.ok(customerService.getCustomersByStoreEntity_Id(userDto.getStoreId()));
        }
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<?> getCustomersByStore(@PathVariable Long storeId,
                                                 @RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer size,
                                                 @RequestParam(required = false) String q) throws Exception
    {
        if(page != null)
        {
            return ResponseEntity.ok(customerService.getCustomersByStorePaged(
                    storeId, page, size != null ? size : 20, q));
        }
        return ResponseEntity.ok(customerService.getCustomersByStoreEntity_Id(storeId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CustomerEntity>> searchCustomer(@RequestParam String keyword)
    {
        return ResponseEntity.ok(customerService.searchCustomer(keyword));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerEntity> getCustomerById(@PathVariable Long id) throws Exception
    {
        return ResponseEntity.ok(customerService.getCustomer(id));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse> deleteAllCustomers()
    {
        customerService.deleteAllCustomers();
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("All customers deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }
}
