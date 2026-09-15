package com.renko.controller;

import com.renko.payload.dto.ProductDto;
import com.renko.payload.dto.UserDto;
import com.renko.payload.dto.updates.ProductUpdateDto;
import com.renko.payload.response.ApiResponse;
import com.renko.service.ProductService;
import com.renko.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController
{
    private final ProductService productService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto productDto,
                                                    @RequestHeader("Authorization") String jwt) throws Exception
    {
        UserDto userDto = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(productService.createProduct(productDto, userDto));
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts()
    {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) throws Exception
    {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<?> getByStoreId(@PathVariable Long storeId,
                                          @RequestParam(required = false) Integer page,
                                          @RequestParam(required = false) Integer size,
                                          @RequestParam(required = false) String q) throws Exception
    {
        if(page != null)
        {
            return ResponseEntity.ok(productService.getProductsByStoreIdPaged(
                    storeId, page, size != null ? size : 20, q));
        }
        return ResponseEntity.ok(productService.getProductsByStoreId(storeId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id,
                                                    @RequestBody ProductUpdateDto productDto) throws Exception
    {
        return ResponseEntity.ok(productService.updateProduct(id, productDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteProduct(@PathVariable Long id,
                                                     @RequestHeader("Authorization") String jwt) throws Exception
    {
        UserDto userDto = userService.getUserFromJwtToken(jwt);
        productService.deleteProduct(id, userDto);
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Product deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse> deleteAllProducts()
    {
        productService.deleteAllProducts();
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("All products deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/store/{storeId}/search")
    public ResponseEntity<List<ProductDto>> searchByKeyword(@PathVariable Long storeId,
                                                           @RequestParam String keyword) throws Exception
    {
        return ResponseEntity.ok(productService.searchByKeyword(storeId, keyword));
    }
}
