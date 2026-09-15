package com.renko.service.impl;

import com.renko.entities.OrderEntity;
import com.renko.entities.RefundEntity;
import com.renko.exceptions.ExceptionMessages;
import com.renko.mapper.RefundMapper;
import com.renko.payload.dto.RefundDto;
import com.renko.payload.dto.UserDto;
import com.renko.repository.OrderRepository;
import com.renko.repository.RefundRepository;
import com.renko.repository.ShiftReportRepository;
import com.renko.service.AuditLogService;
import com.renko.service.RefundService;
import com.renko.service.StoreAccessService;
import com.renko.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService
{
    private final RefundRepository refundRepository;
    private final UserService userService;
    private final OrderRepository orderRepository;
    private final ShiftReportRepository shiftReportRepository;
    private final StoreAccessService storeAccessService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public RefundDto createRefund(RefundDto refundDto) throws Exception
    {
        if(refundDto.getOrderId() == null)
        {
            throw ExceptionMessages.required("orderId", "orderId is required to create a refund");
        }
        OrderEntity order = orderRepository.findDetailedById(refundDto.getOrderId())
                .orElseThrow(() -> ExceptionMessages.notFound("Order", refundDto.getOrderId(), "create refund"));
        storeAccessService.requireStoreAccess(order.getStoreId());

        UserDto cashier = userService.getCurrentUser();
        RefundEntity refund = RefundEntity.builder()
                .order(order)
                .reason(refundDto.getReason())
                .amount(refundDto.getAmount() != null ? refundDto.getAmount() : order.getTotalAmount())
                .cashierId(cashier.getId())
                .storeId(order.getStoreId())
                .paymentType(order.getPaymentType())
                .build();

        shiftReportRepository.findTopByCashierIdAndShiftEndIsNullOrderByShiftStartDesc(cashier.getId())
                .ifPresent(refund::setShiftReportEntity);

        RefundEntity saved = refundRepository.save(refund);
        auditLogService.record(order.getStoreId(), "REFUND_CREATE", "Refund", String.valueOf(saved.getId()),
                "orderId=" + order.getId() + "; amount=" + saved.getAmount());
        return RefundMapper.toDto(saved);
    }

    @Override
    public List<RefundDto> getAllRefunds()
    {
        return refundRepository.findAll().stream().map(RefundMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<RefundDto> getRefundsByCashier(Long cashierId)
    {
        return refundRepository.findByCashierId(cashierId).stream().map(RefundMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<RefundDto> getRefundByShiftReport(Long shiftReportId)
    {
        return refundRepository.findByShiftReportEntity_Id(shiftReportId).stream().map(RefundMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<RefundDto> getRefundByCashierAndDateRange(Long cashierId, LocalDateTime start, LocalDateTime end)
    {
        return refundRepository.findByCashierIdAndCreatedAtBetween(cashierId, start, end).stream()
                .map(RefundMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<RefundDto> getRefundsByStore(Long storeId) throws Exception
    {
        storeAccessService.requireStoreAccess(storeId);
        return refundRepository.findByStoreId(storeId).stream().map(RefundMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public RefundDto getRefundById(Long refundId) throws Exception
    {
        return RefundMapper.toDto(refundRepository.findById(refundId)
                .orElseThrow(() -> ExceptionMessages.notFound("Refund", refundId)));
    }

    @Override
    public void deleteRefund(Long refundId)
    {
        RefundEntity refund = refundRepository.findById(refundId)
                .orElseThrow(() -> ExceptionMessages.notFound("Refund", refundId, "delete"));
        refundRepository.delete(refund);
    }

    @Override
    public void deleteAllRefunds()
    {
        refundRepository.deleteAll();
    }
}
