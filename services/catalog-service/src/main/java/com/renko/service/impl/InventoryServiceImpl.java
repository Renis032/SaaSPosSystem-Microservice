package com.renko.service.impl;

import com.renko.entities.InventoryEntity;
import com.renko.entities.ProductEntity;
import com.renko.exceptions.ExceptionMessages;
import com.renko.exceptions.UserException;
import com.renko.mapper.InventoryMapper;
import com.renko.payload.dto.InventoryDto;
import com.renko.payload.dto.updates.InventoryUpdateDto;
import com.renko.payload.response.ApiResponse;
import com.renko.repository.InventoryRepository;
import com.renko.repository.ProductRepository;
import com.renko.service.AuditLogService;
import com.renko.service.InventoryService;
import com.renko.service.StoreAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService
{
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final StoreAccessService storeAccessService;
    private final AuditLogService auditLogService;

    @Override
    public InventoryDto createInventory(InventoryDto inventoryDto) throws Exception
    {
        if(inventoryDto.getStoreId() == null)
        {
            throw ExceptionMessages.required("storeId", "storeId is required to create inventory");
        }
        if(inventoryDto.getProductId() == null)
        {
            throw ExceptionMessages.required("productId", "productId is required to create inventory");
        }
        storeAccessService.requireStoreAccess(inventoryDto.getStoreId());
        ProductEntity productEntity = productRepository.findById(inventoryDto.getProductId())
                .orElseThrow(() -> ExceptionMessages.notFound("Product", inventoryDto.getProductId(), "create inventory"));
        assertProductBelongsToStore(productEntity, inventoryDto.getStoreId(), "create inventory");
        InventoryEntity saved = inventoryRepository.save(
                InventoryMapper.toEntity(inventoryDto, inventoryDto.getStoreId(), productEntity));
        return InventoryMapper.toDto(saved);
    }

    @Override
    public InventoryDto updateInventory(Long id, InventoryUpdateDto inventoryDto) throws Exception
    {
        InventoryEntity inventoryEntity = inventoryRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Inventory", id, "update"));
        if(inventoryDto.getStoreId() != null)
        {
            storeAccessService.requireStoreAccess(inventoryDto.getStoreId());
            inventoryEntity.setStoreId(inventoryDto.getStoreId());
        }
        if(inventoryDto.getProductId() != null)
        {
            ProductEntity productEntity = productRepository.findById(inventoryDto.getProductId())
                    .orElseThrow(() -> ExceptionMessages.notFound("Product", inventoryDto.getProductId(), "update inventory"));
            assertProductBelongsToStore(productEntity, inventoryEntity.getStoreId(), "update inventory");
            inventoryEntity.setProductEntity(productEntity);
        }
        if(inventoryDto.getQuantity() != null) inventoryEntity.setQuantity(inventoryDto.getQuantity());
        if(inventoryDto.getLowStockThreshold() != null) inventoryEntity.setLowStockThreshold(inventoryDto.getLowStockThreshold());
        inventoryEntity.setLastUpdated(LocalDateTime.now());
        return InventoryMapper.toDto(inventoryRepository.save(inventoryEntity));
    }

    @Override
    public ApiResponse deleteInventory(Long id) throws Exception
    {
        InventoryEntity inventoryEntity = inventoryRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Inventory", id, "delete"));
        inventoryRepository.delete(inventoryEntity);
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Inventory deleted successfully");
        return apiResponse;
    }

    @Override
    public void deleteAllInventories() { inventoryRepository.deleteAll(); }

    @Override
    public InventoryDto getInventoryById(Long id) throws Exception
    {
        return InventoryMapper.toDto(inventoryRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Inventory", id)));
    }

    @Override
    public List<InventoryDto> getAllInventories()
    {
        return inventoryRepository.findAll().stream().map(InventoryMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<InventoryDto> getInventoryByStoreId(Long storeId) throws Exception
    {
        storeAccessService.requireStoreAccess(storeId);
        return inventoryRepository.findByStoreId(storeId).stream().map(InventoryMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<InventoryDto> getLowStockByStoreId(Long storeId) throws Exception
    {
        storeAccessService.requireStoreAccess(storeId);
        return inventoryRepository.findLowStockByStoreId(storeId).stream()
                .map(InventoryMapper::toDto)
                .sorted((a, b) -> Integer.compare(a.getQuantity(), b.getQuantity()))
                .collect(Collectors.toList());
    }

    @Override
    public InventoryDto getInventoryByStoreIdAndProductId(Long storeId, Long productId) throws Exception
    {
        storeAccessService.requireStoreAccess(storeId);
        InventoryEntity inventoryEntity = inventoryRepository.findByStoreIdAndProductEntity_Id(storeId, productId);
        return inventoryEntity != null ? InventoryMapper.toDto(inventoryEntity) : null;
    }

    @Override
    public InventoryDto updateLowStockThreshold(Long id, Integer threshold) throws Exception
    {
        InventoryEntity inventoryEntity = inventoryRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Inventory", id, "set threshold"));
        inventoryEntity.setLowStockThreshold(threshold);
        return InventoryMapper.toDto(inventoryRepository.save(inventoryEntity));
    }

    @Override
    public InventoryDto addStock(Long id, Integer quantity) throws Exception
    {
        InventoryEntity inventoryEntity = inventoryRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Inventory", id, "addStock"));
        if(inventoryEntity.getStoreId() != null) storeAccessService.requireStoreAccess(inventoryEntity.getStoreId());
        int previous = inventoryEntity.getQuantity();
        inventoryEntity.setQuantity(inventoryEntity.getQuantity() + quantity);
        InventoryEntity updated = inventoryRepository.save(inventoryEntity);
        auditLogService.recordChange(inventoryEntity.getStoreId(), "INVENTORY_ADD", "Inventory",
                String.valueOf(id), "qty=" + previous, "qty=" + updated.getQuantity(), "Added quantity=" + quantity);
        return InventoryMapper.toDto(updated);
    }

    @Override
    public InventoryDto adjustStock(Long id, Integer delta, String reason) throws Exception
    {
        if(delta == null || delta == 0)
        {
            throw ExceptionMessages.required("delta", "Stock adjustment delta must be a non-zero integer");
        }
        InventoryEntity inventoryEntity = inventoryRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Inventory", id, "adjust stock"));
        if(inventoryEntity.getStoreId() != null) storeAccessService.requireStoreAccess(inventoryEntity.getStoreId());
        int next = inventoryEntity.getQuantity() + delta;
        if(next < 0)
        {
            throw UserException.withDetails("Adjustment would make stock negative",
                    ExceptionMessages.ctx("inventoryId", id, "current", inventoryEntity.getQuantity(), "delta", delta));
        }
        int previous = inventoryEntity.getQuantity();
        inventoryEntity.setQuantity(next);
        InventoryEntity saved = inventoryRepository.save(inventoryEntity);
        auditLogService.recordChange(inventoryEntity.getStoreId(), "INVENTORY_ADJUST", "Inventory",
                String.valueOf(id), "qty=" + previous, "qty=" + next,
                "delta=" + delta + (reason != null ? "; reason=" + reason : ""));
        return InventoryMapper.toDto(saved);
    }

    @Override
    @Transactional
    public InventoryDto deductStock(Long storeId, Long productId, Integer quantity) throws Exception
    {
        if(quantity == null || quantity <= 0)
        {
            throw ExceptionMessages.required("quantity", "quantity must be > 0");
        }
        storeAccessService.requireStoreAccess(storeId);
        InventoryEntity inventory = inventoryRepository.findByStoreAndProductForUpdate(storeId, productId)
                .orElseThrow(() -> UserException.withDetails(
                        "Product has no inventory row for this store; create inventory before ordering",
                        ExceptionMessages.ctx("productId", productId, "storeId", storeId)));
        if(inventory.getQuantity() < quantity)
        {
            throw UserException.withDetails("Insufficient stock for product",
                    ExceptionMessages.ctx("productId", productId, "storeId", storeId,
                            "available", inventory.getQuantity(), "requested", quantity));
        }
        int previous = inventory.getQuantity();
        inventory.setQuantity(previous - quantity);
        InventoryEntity saved = inventoryRepository.save(inventory);
        auditLogService.recordChange(storeId, "INVENTORY_DEDUCT", "Inventory", String.valueOf(saved.getId()),
                "qty=" + previous, "qty=" + saved.getQuantity(), "deducted=" + quantity + "; productId=" + productId);
        return InventoryMapper.toDto(saved);
    }

    private void assertProductBelongsToStore(ProductEntity productEntity, Long storeId, String action) throws Exception
    {
        if(storeId == null) return;
        if(productEntity.getStoreId() == null || !storeId.equals(productEntity.getStoreId()))
        {
            throw ExceptionMessages.mismatch(
                    "Product does not belong to the inventory store; cannot " + action,
                    "productId", productEntity.getId(),
                    "productStoreId", productEntity.getStoreId(),
                    "storeId", storeId);
        }
    }
}
