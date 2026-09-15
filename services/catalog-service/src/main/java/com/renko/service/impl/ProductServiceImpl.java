package com.renko.service.impl;

import com.renko.entities.CategoryEntity;
import com.renko.entities.ProductEntity;
import com.renko.exceptions.ExceptionMessages;
import com.renko.mapper.ProductMapper;
import com.renko.payload.dto.PageResponse;
import com.renko.payload.dto.ProductDto;
import com.renko.payload.dto.UserDto;
import com.renko.payload.dto.updates.ProductUpdateDto;
import com.renko.repository.CategoryRepository;
import com.renko.repository.ProductRepository;
import com.renko.service.AuditLogService;
import com.renko.service.ProductService;
import com.renko.service.StoreAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService
{
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StoreAccessService storeAccessService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public ProductDto createProduct(ProductDto productDto, UserDto user) throws Exception
    {
        Long storeId = productDto.getStoreId() != null ? productDto.getStoreId() : user.getStoreId();
        if(storeId == null)
        {
            throw ExceptionMessages.required("storeId", "storeId is required to create a product");
        }
        storeAccessService.requireStoreAccess(storeId);

        CategoryEntity category = null;
        if(productDto.getCategoryId() != null)
        {
            category = categoryRepository.findById(productDto.getCategoryId())
                    .orElseThrow(() -> ExceptionMessages.notFound("Category", productDto.getCategoryId(), "create product"));
            if(category.getStoreId() != null && !storeId.equals(category.getStoreId()))
            {
                throw ExceptionMessages.mismatch("Category does not belong to store",
                        "categoryId", category.getId(), "categoryStoreId", category.getStoreId(), "storeId", storeId);
            }
        }

        ProductEntity saved = productRepository.save(ProductMapper.toEntity(productDto, storeId, category));
        auditLogService.record(storeId, "PRODUCT_CREATE", "Product", String.valueOf(saved.getId()),
                "name=" + saved.getName() + "; sku=" + saved.getSku());
        return ProductMapper.toDto(saved);
    }

    @Override
    public ProductDto getProductById(Long id) throws Exception
    {
        return ProductMapper.toDto(productRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Product", id)));
    }

    @Override
    public List<ProductDto> getAllProducts()
    {
        return productRepository.findAll().stream().map(ProductMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductDto updateProduct(Long id, ProductUpdateDto productDto) throws Exception
    {
        ProductEntity existing = productRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Product", id, "update"));
        if(existing.getStoreId() != null)
        {
            storeAccessService.requireStoreAccess(existing.getStoreId());
        }
        String before = "name=" + existing.getName() + "; sellingPrice=" + existing.getSellingPrice();
        existing.updateFrom(productDto);
        if(productDto.getCategoryId() != null)
        {
            CategoryEntity category = categoryRepository.findById(productDto.getCategoryId())
                    .orElseThrow(() -> ExceptionMessages.notFound("Category", productDto.getCategoryId(), "update product"));
            existing.setCategoryEntity(category);
        }
        ProductEntity saved = productRepository.save(existing);
        auditLogService.recordChange(saved.getStoreId(), "PRODUCT_UPDATE", "Product", String.valueOf(saved.getId()),
                before, "name=" + saved.getName() + "; sellingPrice=" + saved.getSellingPrice(), null);
        return ProductMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id, UserDto user) throws Exception
    {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Product", id, "delete"));
        if(product.getStoreId() != null)
        {
            storeAccessService.requireStoreAccess(product.getStoreId());
        }
        productRepository.delete(product);
    }

    @Override
    public void deleteAllProducts()
    {
        productRepository.deleteAll();
    }

    @Override
    public List<ProductDto> getProductsByStoreId(Long storeId) throws Exception
    {
        storeAccessService.requireStoreAccess(storeId);
        return productRepository.findByStoreId(storeId).stream().map(ProductMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<ProductDto> searchByKeyword(Long storeId, String keyword) throws Exception
    {
        storeAccessService.requireStoreAccess(storeId);
        String q = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return productRepository.searchByStore(storeId, q, PageRequest.of(0, 100)).stream()
                .map(ProductMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public PageResponse<ProductDto> getProductsByStoreIdPaged(Long storeId, int page, int size, String q) throws Exception
    {
        storeAccessService.requireStoreAccess(storeId);
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        String query = q == null || q.isBlank() ? null : q.trim();
        Page<ProductEntity> result = productRepository.searchByStore(
                storeId, query, PageRequest.of(safePage, safeSize, Sort.by("name")));
        return PageResponse.of(
                result.getContent().stream().map(ProductMapper::toDto).collect(Collectors.toList()),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }
}
