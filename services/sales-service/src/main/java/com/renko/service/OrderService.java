package com.renko.service;

import com.renko.domain.OrderStatus;
import com.renko.domain.PaymentType;
import com.renko.payload.dto.OrderDto;
import com.renko.payload.dto.PageResponse;
import com.renko.payload.dto.ReceiptDto;

import java.util.List;

public interface OrderService
{
    OrderDto createOrder(OrderDto orderDto) throws Exception;
    OrderDto updateOrder(Long id, OrderDto orderDto) throws Exception;
    OrderDto getOrderById(Long id);
    List<OrderDto> getAllOrders();

    List<OrderDto> getOrdersByStore(Long storeId,
                                    Long customerId,
                                    Long cashierId,
                                    PaymentType paymentType,
                                    OrderStatus orderStatus) throws Exception;

    PageResponse<OrderDto> getOrdersByStorePaged(Long storeId, int page, int size, String q) throws Exception;

    List<OrderDto> getOrdersByCashier(Long cashierId);

    void deleteOrder(Long id);
    void deleteAllOrders();

    List<OrderDto> getOrdersByCustomerId(Long customerId);

    List<OrderDto> getTodayOrdersByStore(Long storeId) throws Exception;

    List<OrderDto> getTop5RecentOrdersByStoreId(Long storeId) throws Exception;

    ReceiptDto getReceipt(Long orderId);
}
