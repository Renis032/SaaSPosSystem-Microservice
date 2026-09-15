package com.renko.controller;

import com.renko.domain.OrderStatus;
import com.renko.domain.PaymentType;
import com.renko.payload.dto.OrderDto;
import com.renko.payload.dto.ReceiptDto;
import com.renko.payload.response.ApiResponse;
import com.renko.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController
{
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody OrderDto orderDto) throws Exception
    {
        return ResponseEntity.ok(orderService.createOrder(orderDto));
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getAllOrders()
    {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long id)
    {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<?> getOrdersByStore(@PathVariable Long storeId,
                                              @RequestParam(required = false) Long customerId,
                                              @RequestParam(required = false) Long cashierId,
                                              @RequestParam(required = false) PaymentType paymentType,
                                              @RequestParam(required = false) OrderStatus orderStatus,
                                              @RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer size,
                                              @RequestParam(required = false) String q) throws Exception
    {
        if(page != null)
        {
            return ResponseEntity.ok(orderService.getOrdersByStorePaged(
                    storeId, page, size != null ? size : 20, q));
        }
        return ResponseEntity.ok(orderService.getOrdersByStore(storeId, customerId, cashierId, paymentType, orderStatus));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderDto>> getOrdersByCustomer(@PathVariable Long customerId)
    {
        return ResponseEntity.ok(orderService.getOrdersByCustomerId(customerId));
    }

    @GetMapping("/today/store/{storeId}")
    public ResponseEntity<List<OrderDto>> getTodayOrdersByStore(@PathVariable Long storeId) throws Exception
    {
        return ResponseEntity.ok(orderService.getTodayOrdersByStore(storeId));
    }

    @GetMapping("/recent/store/{storeId}")
    public ResponseEntity<List<OrderDto>> getRecentOrders(@PathVariable Long storeId) throws Exception
    {
        return ResponseEntity.ok(orderService.getTop5RecentOrdersByStoreId(storeId));
    }

    @GetMapping("/cashier/{id}")
    public ResponseEntity<List<OrderDto>> getOrdersByCashier(@PathVariable Long id)
    {
        return ResponseEntity.ok(orderService.getOrdersByCashier(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderDto> updateOrder(@PathVariable Long id,
                                                @RequestBody OrderDto orderDto) throws Exception
    {
        return ResponseEntity.ok(orderService.updateOrder(id, orderDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteOrder(@PathVariable Long id)
    {
        orderService.deleteOrder(id);

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Order deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse> deleteAllOrders()
    {
        orderService.deleteAllOrders();

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("All orders deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/{id}/receipt")
    public ResponseEntity<ReceiptDto> getReceipt(@PathVariable Long id)
    {
        return ResponseEntity.ok(orderService.getReceipt(id));
    }
}
