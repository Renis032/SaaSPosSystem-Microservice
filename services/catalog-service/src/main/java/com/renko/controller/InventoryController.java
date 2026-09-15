package com.renko.controller;

import com.renko.payload.dto.InventoryDto;
import com.renko.payload.dto.updates.InventoryUpdateDto;
import com.renko.payload.response.ApiResponse;
import com.renko.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventories")
public class InventoryController
{
    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<InventoryDto> create(@RequestBody InventoryDto inventoryDto) throws Exception
    {
        return ResponseEntity.ok(inventoryService.createInventory(inventoryDto));
    }

    @GetMapping
    public ResponseEntity<List<InventoryDto>> getAllInventories()
    {
        return ResponseEntity.ok(inventoryService.getAllInventories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryDto> getInventoryById(@PathVariable Long id) throws Exception
    {
        return ResponseEntity.ok(inventoryService.getInventoryById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryDto> update(@RequestBody InventoryUpdateDto inventoryDto,
                                               @PathVariable Long id) throws Exception
    {
        return ResponseEntity.ok(inventoryService.updateInventory(id, inventoryDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) throws Exception
    {
        return ResponseEntity.ok(inventoryService.deleteInventory(id));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse> deleteAllInventories()
    {
        inventoryService.deleteAllInventories();

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("All inventories deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<InventoryDto>> getInventoryByStoreId(@PathVariable Long storeId) throws Exception
    {
        return ResponseEntity.ok(inventoryService.getInventoryByStoreId(storeId));
    }

    @GetMapping("/store/{storeId}/product/{productId}")
    public ResponseEntity<InventoryDto> getInventoryByStoreIdAndProductId(@PathVariable Long storeId,
                                                                          @PathVariable Long productId) throws Exception
    {
        return ResponseEntity.ok(inventoryService.getInventoryByStoreIdAndProductId(storeId, productId));
    }

    @GetMapping("/store/{storeId}/low-stock")
    public ResponseEntity<List<InventoryDto>> getLowStockInventory(@PathVariable Long storeId) throws Exception
    {
        return ResponseEntity.ok(inventoryService.getLowStockByStoreId(storeId));
    }

    @PatchMapping("/{id}/threshold")
    public ResponseEntity<InventoryDto> updateThreshold(@PathVariable Long id,
                                                        @RequestParam Integer threshold) throws Exception
    {
        return ResponseEntity.ok(inventoryService.updateLowStockThreshold(id, threshold));
    }

    @PostMapping("/{id}/add-stock")
    @PreAuthorize("hasAnyRole('OWNER','STORE_MANAGER','BRANCH_MANAGER','ADMIN')")
    public ResponseEntity<InventoryDto> addStock(@PathVariable Long id,
                                                 @RequestParam Integer quantity) throws Exception
    {
        return ResponseEntity.ok(inventoryService.addStock(id, quantity));
    }

    @PostMapping("/store/{storeId}/product/{productId}/deduct")
    public ResponseEntity<InventoryDto> deduct(@PathVariable Long storeId,
                                               @PathVariable Long productId,
                                               @RequestParam Integer quantity) throws Exception
    {
        return ResponseEntity.ok(inventoryService.deductStock(storeId, productId, quantity));
    }

    @PostMapping("/{id}/adjust")
    @PreAuthorize("hasAnyRole('OWNER','STORE_MANAGER','BRANCH_MANAGER','ADMIN')")
    public ResponseEntity<InventoryDto> adjustStock(@PathVariable Long id,
                                                    @RequestParam Integer delta,
                                                    @RequestParam(required = false) String reason) throws Exception
    {
        return ResponseEntity.ok(inventoryService.adjustStock(id, delta, reason));
    }
}
