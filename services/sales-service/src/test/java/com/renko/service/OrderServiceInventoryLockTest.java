package com.renko.service;

import com.renko.client.BillingServiceClient;
import com.renko.client.CatalogServiceClient;
import com.renko.domain.PaymentType;
import com.renko.domain.UserRole;
import com.renko.entities.OrderEntity;
import com.renko.payload.dto.InventoryDto;
import com.renko.payload.dto.OrderDto;
import com.renko.payload.dto.OrderItemDto;
import com.renko.payload.dto.ProductDto;
import com.renko.payload.dto.UserDto;
import com.renko.repository.OrderRepository;
import com.renko.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceInventoryLockTest
{
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserService userService;
    @Mock
    private CatalogServiceClient catalogServiceClient;
    @Mock
    private BillingServiceClient billingServiceClient;
    @Mock
    private CustomerService customerService;
    @Mock
    private StoreAccessService storeAccessService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void cashierInStore() throws Exception
    {
        when(userService.getCurrentUser()).thenReturn(cashier());
    }

    @Test
    void createOrderDeductsStockForEachLine() throws Exception
    {
        when(catalogServiceClient.getProduct(10L)).thenReturn(product(10L, 9.99));
        when(catalogServiceClient.deduct(1L, 10L, 2)).thenReturn(new InventoryDto());
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> {
            OrderEntity order = inv.getArgument(0);
            order.setId(50L);
            return order;
        });
        when(orderRepository.findDetailedById(50L)).thenAnswer(inv -> {
            OrderEntity saved = new OrderEntity();
            saved.setId(50L);
            saved.setStoreId(1L);
            saved.setTotalAmount(19.98);
            saved.setItems(List.of());
            return Optional.of(saved);
        });

        OrderDto created = orderService.createOrder(cashOrder(10L, 2));

        assertEquals(50L, created.getId());
        verify(catalogServiceClient).deduct(1L, 10L, 2);
        verify(orderRepository).save(any(OrderEntity.class));
    }

    @Test
    void createOrderDoesNotSaveWhenDeductFails() throws Exception
    {
        when(catalogServiceClient.getProduct(10L)).thenReturn(product(10L, 9.99));
        when(catalogServiceClient.deduct(eq(1L), eq(10L), eq(2)))
                .thenThrow(new RuntimeException("Insufficient stock for product"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.createOrder(cashOrder(10L, 2)));
        assertEquals("Insufficient stock for product", ex.getMessage());
        verify(catalogServiceClient).deduct(1L, 10L, 2);
        verify(orderRepository, never()).save(any());
    }

    private static OrderDto cashOrder(Long productId, int qty)
    {
        return OrderDto.builder()
                .paymentType(PaymentType.CASH)
                .items(List.of(OrderItemDto.builder().productId(productId).quantity(qty).build()))
                .build();
    }

    private static ProductDto product(Long id, double price)
    {
        ProductDto dto = new ProductDto();
        dto.setId(id);
        dto.setName("Espresso");
        dto.setSku("DEMO-ESP");
        dto.setSellingPrice(price);
        dto.setDiscountPercentage(0.0);
        dto.setStoreId(1L);
        return dto;
    }

    private static UserDto cashier()
    {
        UserDto dto = new UserDto();
        dto.setId(2L);
        dto.setStoreId(1L);
        dto.setRole(UserRole.CASHIER);
        dto.setEmail("cashier@renko.demo");
        return dto;
    }
}
