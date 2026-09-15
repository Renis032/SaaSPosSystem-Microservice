package com.renko.service.impl;

import com.renko.client.BillingServiceClient;
import com.renko.client.CatalogServiceClient;
import com.renko.domain.OrderStatus;
import com.renko.domain.PaymentType;
import com.renko.entities.*;
import com.renko.exceptions.ExceptionMessages;
import com.renko.mapper.OrderMapper;
import com.renko.payload.dto.*;
import com.renko.repository.OrderRepository;
import com.renko.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService
{
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final CatalogServiceClient catalogServiceClient;
    private final BillingServiceClient billingServiceClient;
    private final CustomerService customerService;
    private final StoreAccessService storeAccessService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public OrderDto createOrder(OrderDto orderDto) throws Exception
    {
        UserDto cashier = userService.getCurrentUser();
        if(cashier.getStoreId() == null)
        {
            throw ExceptionMessages.required("storeId",
                    "Cashier has no linked store; cannot create order. Assign the cashier to a store first.");
        }
        Long storeId = cashier.getStoreId();
        billingServiceClient.requireActiveSubscription(storeId);

        if(orderDto.getItems() == null || orderDto.getItems().isEmpty())
        {
            throw ExceptionMessages.required("items", "Order items are required.");
        }

        CustomerEntity customer = null;
        if(orderDto.getCustomerId() != null)
        {
            customer = customerService.getCustomer(orderDto.getCustomerId());
            if(customer.getStoreId() != null && !storeId.equals(customer.getStoreId()))
            {
                throw ExceptionMessages.mismatch("Customer does not belong to the cashier's store",
                        "customerId", customer.getId(), "customerStoreId", customer.getStoreId(), "storeId", storeId);
            }
        }
        else if(orderDto.getCustomerPhone() != null && !orderDto.getCustomerPhone().isEmpty())
        {
            List<CustomerEntity> existing = customerService.searchCustomer(orderDto.getCustomerPhone());
            if(!existing.isEmpty())
            {
                customer = existing.get(0);
            }
            else
            {
                customer = new CustomerEntity();
                customer.setFullName(orderDto.getCustomerName() != null ? orderDto.getCustomerName() : "Guest");
                customer.setPhone(orderDto.getCustomerPhone());
                customer.setStoreId(storeId);
                customer = customerService.createCustomer(customer);
            }
        }

        OrderEntity order = OrderEntity.builder()
                .storeId(storeId)
                .cashierId(cashier.getId())
                .customerEntity(customer)
                .paymentType(orderDto.getPaymentType())
                .branchId(orderDto.getBranchId())
                .build();

        List<OrderItemEntity> orderItems = new java.util.ArrayList<>();
        for(OrderItemDto itemDto : orderDto.getItems())
        {
            if(itemDto.getProductId() == null)
            {
                throw ExceptionMessages.required("productId", "productId is required for each order item");
            }
            if(itemDto.getQuantity() == null || itemDto.getQuantity() <= 0)
            {
                throw new IllegalArgumentException("Order item quantity must be greater than 0 for productId=" + itemDto.getProductId());
            }

            ProductDto product = catalogServiceClient.getProduct(itemDto.getProductId());
            if(product == null)
            {
                throw ExceptionMessages.notFound("Product", itemDto.getProductId(), "add order item");
            }
            if(product.getStoreId() == null || !storeId.equals(product.getStoreId()))
            {
                throw ExceptionMessages.mismatch("Product does not belong to the cashier's store",
                        "productId", product.getId(), "productStoreId", product.getStoreId(), "storeId", storeId);
            }

            double originalPrice = product.getSellingPrice() != null ? product.getSellingPrice() : 0.0;
            double discountPercentage = itemDto.getDiscountPercent() != null
                    ? itemDto.getDiscountPercent()
                    : (product.getDiscountPercentage() != null ? product.getDiscountPercentage() : 0.0);
            if(discountPercentage < 0 || discountPercentage > 100)
            {
                throw new IllegalArgumentException("discountPercent must be between 0 and 100");
            }
            double discountAmount = (originalPrice * discountPercentage) / 100.0;
            double discountedPrice = originalPrice - discountAmount;

            orderItems.add(OrderItemEntity.builder()
                    .quantity(itemDto.getQuantity())
                    .price(discountedPrice * itemDto.getQuantity())
                    .originalPrice(originalPrice * itemDto.getQuantity())
                    .discountApplied(discountAmount * itemDto.getQuantity())
                    .productId(product.getId())
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .productDiscountPercentage(product.getDiscountPercentage())
                    .orderEntity(order)
                    .build());
        }

        double subtotal = orderItems.stream().mapToDouble(OrderItemEntity::getOriginalPrice).sum();
        double lineDiscount = orderItems.stream().mapToDouble(OrderItemEntity::getDiscountApplied).sum();
        double afterLineDiscount = subtotal - lineDiscount;
        double orderDiscountPercent = orderDto.getOrderDiscountPercent() != null ? orderDto.getOrderDiscountPercent() : 0.0;
        if(orderDiscountPercent < 0 || orderDiscountPercent > 100)
        {
            throw new IllegalArgumentException("orderDiscountPercent must be between 0 and 100");
        }
        double orderLevelDiscount = afterLineDiscount * orderDiscountPercent / 100.0;
        double taxable = afterLineDiscount - orderLevelDiscount;
        double taxRate = orderDto.getTaxRate() != null ? orderDto.getTaxRate() : 0.0;
        if(taxRate < 0 || taxRate > 100)
        {
            throw new IllegalArgumentException("taxRate must be between 0 and 100");
        }
        double taxAmount = taxable * taxRate / 100.0;

        order.setSubtotal(subtotal);
        order.setTotalDiscount(lineDiscount + orderLevelDiscount);
        order.setOrderDiscountPercent(orderDiscountPercent);
        order.setTaxRate(taxRate);
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(taxable + taxAmount);
        order.setItems(orderItems);

        if(orderDto.getPaymentType() == PaymentType.CARD)
        {
            String intentId = orderDto.getStripePaymentIntentId();
            if(intentId == null || intentId.isBlank())
            {
                throw new Exception("CARD payment requires stripePaymentIntentId");
            }
            boolean demoCard = intentId.startsWith("demo_");
            if(!demoCard && !billingServiceClient.verifyPayment(intentId))
            {
                throw new Exception("CARD payment not confirmed for stripePaymentIntentId=" + intentId);
            }
            order.setStripePaymentIntentId(intentId);
        }

        for(OrderItemEntity item : orderItems)
        {
            catalogServiceClient.deduct(storeId, item.getProductId(), item.getQuantity());
        }

        OrderEntity savedOrder = orderRepository.save(order);
        OrderEntity detailed = orderRepository.findDetailedById(savedOrder.getId()).orElse(savedOrder);
        auditLogService.record(storeId, "ORDER_CREATE", "Order", String.valueOf(detailed.getId()),
                "paymentType=" + orderDto.getPaymentType() + "; total=" + detailed.getTotalAmount());
        return OrderMapper.toDto(detailed);
    }

    @Override
    @Transactional
    public OrderDto updateOrder(Long id, OrderDto orderDto) throws Exception
    {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Order", id, "update"));
        if(orderDto.getPaymentType() != null) order.setPaymentType(orderDto.getPaymentType());
        if(orderDto.getCustomerId() != null) order.setCustomerEntity(customerService.getCustomer(orderDto.getCustomerId()));
        if(orderDto.getItems() != null && !orderDto.getItems().isEmpty())
        {
            List<OrderItemEntity> updatedItems = new java.util.ArrayList<>();
            for(OrderItemDto itemsDto : orderDto.getItems())
            {
                if(itemsDto.getProductId() == null)
                {
                    throw ExceptionMessages.required("productId", "Order item productId is required while updating orderId=" + id);
                }
                updatedItems.add(OrderItemEntity.builder()
                        .id(itemsDto.getId())
                        .quantity(itemsDto.getQuantity())
                        .price(itemsDto.getPrice())
                        .originalPrice(itemsDto.getOriginalPrice())
                        .discountApplied(itemsDto.getDiscountApplied())
                        .productId(itemsDto.getProductId())
                        .orderEntity(order)
                        .build());
            }
            order.setItems(updatedItems);
            order.setTotalAmount(updatedItems.stream().mapToDouble(OrderItemEntity::getPrice).sum());
        }
        return OrderMapper.toDto(orderRepository.save(order));
    }

    @Override
    public OrderDto getOrderById(Long id)
    {
        return orderRepository.findDetailedById(id).map(OrderMapper::toDto)
                .orElseThrow(() -> ExceptionMessages.notFound("Order", id));
    }

    @Override
    public List<OrderDto> getAllOrders()
    {
        return orderRepository.findAll().stream().map(OrderMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<OrderDto> getOrdersByStore(Long storeId, Long customerId, Long cashierId,
                                           PaymentType paymentType, OrderStatus orderStatus) throws Exception
    {
        storeAccessService.requireStoreAccess(storeId);
        return orderRepository.findByStoreIdOrderByCreatedAtDesc(storeId).stream()
                .filter(order -> customerId == null || (order.getCustomerEntity() != null && order.getCustomerEntity().getId().equals(customerId)))
                .filter(order -> cashierId == null || (order.getCashierId() != null && order.getCashierId().equals(cashierId)))
                .filter(order -> paymentType == null || order.getPaymentType() == paymentType)
                .map(OrderMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<OrderDto> getOrdersByStorePaged(Long storeId, int page, int size, String q) throws Exception
    {
        storeAccessService.requireStoreAccess(storeId);
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        String query = q == null || q.isBlank() ? null : q.trim();
        Page<OrderEntity> result = orderRepository.searchByStore(storeId, query,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(result.getContent().stream().map(OrderMapper::toDto).collect(Collectors.toList()),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Override
    public List<OrderDto> getOrdersByCashier(Long cashierId)
    {
        return orderRepository.findByCashierId(cashierId).stream().map(OrderMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteOrder(Long id)
    {
        OrderEntity orderEntity = orderRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Order", id, "delete"));
        orderRepository.delete(orderEntity);
    }

    @Override
    @Transactional
    public void deleteAllOrders() { orderRepository.deleteAll(); }

    @Override
    public List<OrderDto> getOrdersByCustomerId(Long customerId)
    {
        return orderRepository.findByCustomerEntity_Id(customerId).stream().map(OrderMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<OrderDto> getTodayOrdersByStore(Long storeId) throws Exception
    {
        storeAccessService.requireStoreAccess(storeId);
        LocalDate today = LocalDate.now();
        return orderRepository.findByStoreIdAndCreatedAtBetween(storeId, today.atStartOfDay(), today.plusDays(1).atStartOfDay())
                .stream().map(OrderMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<OrderDto> getTop5RecentOrdersByStoreId(Long storeId) throws Exception
    {
        storeAccessService.requireStoreAccess(storeId);
        return orderRepository.findTopFiveByStoreIdOrderByCreatedAtDesc(storeId).stream()
                .map(OrderMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptDto getReceipt(Long orderId)
    {
        OrderEntity order = orderRepository.findDetailedById(orderId)
                .orElseThrow(() -> ExceptionMessages.notFound("Order", orderId, "build receipt"));

        String receiptNumber = String.format("RCP-%d-%d", order.getStoreId(), order.getId());
        List<OrderItemEntity> items = order.getItems() != null ? order.getItems() : List.of();
        List<ReceiptItemDto> receiptItems = items.stream().map(item -> {
            int quantity = item.getQuantity();
            double originalPricePerUnit = quantity > 0 ? item.getOriginalPrice() / quantity : 0.0;
            double discountAmountPerUnit = quantity > 0 ? item.getDiscountApplied() / quantity : 0.0;
            double finalPricePerUnit = quantity > 0 ? item.getPrice() / quantity : 0.0;
            return ReceiptItemDto.builder()
                    .name(item.getProductName() != null ? item.getProductName() : "Unknown product")
                    .sku(item.getProductSku() != null ? item.getProductSku() : "")
                    .quantity(quantity)
                    .originalPrice(originalPricePerUnit)
                    .discountPercentage(item.getProductDiscountPercentage() != null ? item.getProductDiscountPercentage() : 0.0)
                    .discountAmount(discountAmountPerUnit)
                    .finalPrice(finalPricePerUnit)
                    .lineTotal(item.getPrice())
                    .build();
        }).collect(Collectors.toList());

        return ReceiptDto.builder()
                .orderId(order.getId())
                .receiptNumber(receiptNumber)
                .orderDate(order.getCreatedAt())
                .storeName(null)
                .storeAddress(null)
                .storePhone(null)
                .cashierName(null)
                .customerName(order.getCustomerEntity() != null ? order.getCustomerEntity().getFullName() : null)
                .customerPhone(order.getCustomerEntity() != null ? order.getCustomerEntity().getPhone() : null)
                .items(receiptItems)
                .subtotal(order.getSubtotal())
                .totalDiscount(order.getTotalDiscount())
                .taxRate(order.getTaxRate())
                .taxAmount(order.getTaxAmount())
                .totalAmount(order.getTotalAmount())
                .branchName(null)
                .paymentType(order.getPaymentType() != null ? order.getPaymentType().name() : null)
                .build();
    }
}
