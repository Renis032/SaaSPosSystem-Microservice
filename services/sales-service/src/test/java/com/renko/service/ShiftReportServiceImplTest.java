package com.renko.service;

import com.renko.domain.UserRole;
import com.renko.entities.ShiftReportEntity;
import com.renko.exceptions.UserException;
import com.renko.payload.dto.ShiftReportDto;
import com.renko.payload.dto.UserDto;
import com.renko.repository.OrderRepository;
import com.renko.repository.RefundRepository;
import com.renko.repository.ShiftReportRepository;
import com.renko.service.impl.ShiftReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShiftReportServiceImplTest
{
    @Mock
    private ShiftReportRepository shiftReportRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private RefundRepository refundRepository;
    @Mock
    private UserService userService;
    @Mock
    private StoreAccessService storeAccessService;

    @InjectMocks
    private ShiftReportServiceImpl shiftReportService;

    @Test
    void currentShiftUsesLoggedInCashierWhenIdOmitted() throws Exception
    {
        when(userService.getCurrentUser()).thenReturn(user(7L));
        ShiftReportEntity open = openShift(11L, 7L);
        when(shiftReportRepository.findTopByCashierIdAndShiftEndIsNullOrderByShiftStartDesc(7L))
                .thenReturn(Optional.of(open));

        ShiftReportDto dto = shiftReportService.getCurrentShiftProgress(null);

        assertEquals(11L, dto.getId());
        verify(shiftReportRepository, never())
                .findTopByCashierIdAndShiftEndIsNullOrderByShiftStartDesc(isNull());
    }

    @Test
    void endShiftWithoutIdClosesLoggedInCashiersOpenShift() throws Exception
    {
        when(userService.getCurrentUser()).thenReturn(user(7L));
        ShiftReportEntity open = openShift(11L, 7L);
        when(shiftReportRepository.findTopByCashierIdAndShiftEndIsNullOrderByShiftStartDesc(7L))
                .thenReturn(Optional.of(open));
        when(orderRepository.findByCashierIdAndCreatedAtBetween(eq(7L), any(), any()))
                .thenReturn(List.of());
        when(refundRepository.findByCashierIdAndCreatedAtBetween(eq(7L), any(), any()))
                .thenReturn(List.of());
        when(shiftReportRepository.save(any(ShiftReportEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ShiftReportDto dto = shiftReportService.endShift(null, LocalDateTime.now());

        assertEquals(11L, dto.getId());
        assertNotNull(dto.getShiftEnd());
        verify(shiftReportRepository, never()).findById(any());
    }

    @Test
    void currentShiftThrowsWhenCashierHasNoneOpen() throws Exception
    {
        when(userService.getCurrentUser()).thenReturn(user(7L));
        when(shiftReportRepository.findTopByCashierIdAndShiftEndIsNullOrderByShiftStartDesc(7L))
                .thenReturn(Optional.empty());

        UserException ex = assertThrows(UserException.class,
                () -> shiftReportService.getCurrentShiftProgress(null));
        assertEquals("No open shift for cashierId=7", ex.getMessage());
    }

    private static UserDto user(Long id)
    {
        UserDto dto = new UserDto();
        dto.setId(id);
        dto.setRole(UserRole.CASHIER);
        dto.setStoreId(1L);
        dto.setEmail("cashier@test.com");
        return dto;
    }

    private static ShiftReportEntity openShift(Long id, Long cashierId)
    {
        return ShiftReportEntity.builder()
                .id(id)
                .cashierId(cashierId)
                .storeId(1L)
                .shiftStart(LocalDateTime.now().minusHours(1))
                .totalSales(0.0)
                .totalRefunds(0.0)
                .netSales(0.0)
                .totalOrders(0)
                .build();
    }
}
