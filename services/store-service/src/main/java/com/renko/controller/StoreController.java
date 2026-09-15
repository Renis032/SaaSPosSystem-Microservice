package com.renko.controller;

import com.renko.domain.StoreStatus;
import com.renko.entities.StoreEntity;
import com.renko.exceptions.UserException;
import com.renko.mapper.StoreMapper;
import com.renko.payload.dto.StoreDto;
import com.renko.payload.dto.UserDto;
import com.renko.payload.response.ApiResponse;
import com.renko.service.StoreService;
import com.renko.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stores")
public class StoreController
{
    private final StoreService storeService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<StoreDto>> getAllStores()
    {
        List<StoreDto> storeDtos = storeService.getAllStores().stream().map(StoreMapper::toDto).toList();
        return ResponseEntity.ok(storeDtos);
    }

    @PostMapping
    public ResponseEntity<StoreDto> createStore(@RequestBody StoreDto storeDto,
                                                @RequestHeader("Authorization") String jwt) throws UserException
    {
        UserDto userDto = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(storeService.createStore(storeDto, userDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoreDto> getStoreById(@PathVariable("id") Long id) throws Exception
    {
        return ResponseEntity.ok(storeService.getStoreById(id));
    }

    @GetMapping("/admin")
    public ResponseEntity<StoreDto> getStoreByAdmin() throws Exception
    {
        StoreEntity store = storeService.getStoreByAdmin();
        if(store == null)
        {
            throw new UserException("No store found for current admin");
        }
        return ResponseEntity.ok(StoreMapper.toDto(store));
    }

    @GetMapping("/employee")
    public ResponseEntity<StoreDto> getStoreByEmployee() throws Exception
    {
        return ResponseEntity.ok(storeService.getStoreByEmployee());
    }

    @PutMapping("/{id}")
    public ResponseEntity<StoreDto> updateStore(@PathVariable("id") Long id,
                                                @RequestBody StoreDto storeDto) throws UserException
    {
        return ResponseEntity.ok(storeService.updateStore(id, storeDto));
    }

    @PutMapping("/{id}/moderate")
    public ResponseEntity<StoreDto> moderateStore(@PathVariable("id") Long id,
                                                  @RequestParam StoreStatus storeStatus) throws Exception
    {
        return ResponseEntity.ok(storeService.moderateStore(id, storeStatus));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteStore(@PathVariable("id") Long id) throws UserException
    {
        storeService.deleteStore(id);
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Store deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse> deleteAllStores() throws UserException
    {
        storeService.deleteAllStores();
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("All stores deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }
}
