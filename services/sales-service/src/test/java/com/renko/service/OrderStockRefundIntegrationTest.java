package com.renko.service;

import com.renko.domain.PaymentType;
import com.renko.domain.UserRole;
import com.renko.entities.OrderEntity;
import com.renko.entities.RefundEntity;
import com.renko.entities.ShiftReportEntity;
import com.renko.payload.dto.RefundDto;
import com.renko.payload.dto.UserDto;
import com.renko.repository.OrderRepository;
import com.renko.repository.RefundRepository;
import com.renko.repository.ShiftReportRepository;
import com.renko.service.impl.RefundServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStockRefundIntegrationTest
{
    @Mock
    private RefundRepository refundRepository;
    @Mock
    private UserService userService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ShiftReportRepository shiftReportRepository;
    @Mock
    private StoreAccessService storeAccessService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private RefundServiceImpl refundService;

    @Test
    void createRefundRequiresOrderId()
    {
        assertThrows(Exception.class, () -> refundService.createRefund(RefundDto.builder().amount(1.0).build()));
    }

    @Test
    void createRefundUsesOrderTotalAndAttachesOpenShift() throws Exception
    {
        OrderEntity order = OrderEntity.builder()
                .id(9L)
                .storeId(1L)
                .totalAmount(10.0)
                .paymentType(PaymentType.CASH)
                .build();
        ShiftReportEntity openShift = ShiftReportEntity.builder()
                .id(3L)
                .cashierId(2L)
                .storeId(1L)
                .shiftStart(LocalDateTime.now().minusHours(1))
                .build();

        when(orderRepository.findDetailedById(9L)).thenReturn(Optional.of(order));
        when(userService.getCurrentUser()).thenReturn(cashier());
        when(shiftReportRepository.findTopByCashierIdAndShiftEndIsNullOrderByShiftStartDesc(2L))
                .thenReturn(Optional.of(openShift));
        when(refundRepository.save(any(RefundEntity.class))).thenAnswer(inv -> {
            RefundEntity refund = inv.getArgument(0);
            refund.setId(40L);
            return refund;
        });

        RefundDto created = refundService.createRefund(RefundDto.builder()
                .orderId(9L)
                .reason("Customer changed mind")
                .build());

        assertEquals(40L, created.getId());
        assertEquals(9L, created.getOrderId());
        assertEquals(10.0, created.getAmount());
        assertEquals(3L, created.getShiftReportId());
        assertEquals(1L, created.getStoreId());

        ArgumentCaptor<RefundEntity> captor = ArgumentCaptor.forClass(RefundEntity.class);
        verify(refundRepository).save(captor.capture());
        assertEquals(openShift, captor.getValue().getShiftReportEntity());
        verify(storeAccessService).requireStoreAccess(1L);
        verify(auditLogService).record(1L, "REFUND_CREATE", "Refund", "40", "orderId=9; amount=10.0");
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
