package com.renko.service.impl;

import com.renko.domain.PaymentType;
import com.renko.entities.OrderEntity;
import com.renko.entities.PaymentSummaryEntity;
import com.renko.entities.RefundEntity;
import com.renko.entities.ShiftReportEntity;
import com.renko.exceptions.ExceptionMessages;
import com.renko.exceptions.UserException;
import com.renko.mapper.ShiftReportMapper;
import com.renko.payload.dto.ShiftReportDto;
import com.renko.payload.dto.UserDto;
import com.renko.repository.OrderRepository;
import com.renko.repository.RefundRepository;
import com.renko.repository.ShiftReportRepository;
import com.renko.service.ShiftReportService;
import com.renko.service.StoreAccessService;
import com.renko.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ShiftReportServiceImpl implements ShiftReportService
{
    private final ShiftReportRepository shiftReportRepository;
    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;
    private final UserService userService;
    private final StoreAccessService storeAccessService;

    @Override
    public ShiftReportDto startShift() throws Exception
    {
        return startShift(null);
    }

    @Override
    public ShiftReportDto startShift(Long branchId) throws Exception
    {
        UserDto currentUser = userService.getCurrentUser();
        if(currentUser.getStoreId() == null)
        {
            throw ExceptionMessages.required("storeId", "Cashier must be linked to a store to start a shift");
        }
        Optional<ShiftReportEntity> existing = shiftReportRepository
                .findTopByCashierIdAndShiftEndIsNullOrderByShiftStartDesc(currentUser.getId());
        if(existing.isPresent())
        {
            throw new UserException("Cashier already has an open shift");
        }

        ShiftReportEntity shift = ShiftReportEntity.builder()
                .shiftStart(LocalDateTime.now())
                .cashierId(currentUser.getId())
                .storeId(currentUser.getStoreId())
                .branchId(branchId)
                .totalSales(0.0)
                .totalRefunds(0.0)
                .netSales(0.0)
                .totalOrders(0)
                .build();
        return ShiftReportMapper.toDto(shiftReportRepository.save(shift));
    }

    @Override
    public ShiftReportDto endShift(Long shiftReportId, LocalDateTime shiftEnd) throws Exception
    {
        ShiftReportEntity shift;
        if(shiftReportId == null)
        {
            shift = requireOpenShift(requireCurrentCashierId());
        }
        else
        {
            shift = shiftReportRepository.findById(shiftReportId)
                    .orElseThrow(() -> ExceptionMessages.notFound("ShiftReport", shiftReportId, "end shift"));
            if(shift.getShiftEnd() != null)
            {
                throw new UserException("Shift is already ended");
            }
        }
        LocalDateTime end = shiftEnd != null ? shiftEnd : LocalDateTime.now();
        shift.setShiftEnd(end);

        List<OrderEntity> orders = orderRepository.findByCashierIdAndCreatedAtBetween(
                shift.getCashierId(), shift.getShiftStart(), end);
        List<RefundEntity> refunds = refundRepository.findByCashierIdAndCreatedAtBetween(
                shift.getCashierId(), shift.getShiftStart(), end);

        double totalSales = orders.stream().mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0).sum();
        double totalRefunds = refunds.stream().mapToDouble(r -> r.getAmount() != null ? r.getAmount() : 0.0).sum();
        shift.setTotalSales(totalSales);
        shift.setTotalRefunds(totalRefunds);
        shift.setNetSales(totalSales - totalRefunds);
        shift.setTotalOrders(orders.size());
        shift.setRecentOrders(orders.stream().sorted(Comparator.comparing(OrderEntity::getCreatedAt).reversed())
                .limit(5).collect(Collectors.toList()));
        shift.setRefunds(refunds);
        shift.setPaymentSummaries(buildPaymentSummaries(orders));

        Map<Long, Integer> productQty = new HashMap<>();
        for(OrderEntity order : orders)
        {
            if(order.getItems() == null) continue;
            for(var item : order.getItems())
            {
                if(item.getProductId() == null) continue;
                productQty.merge(item.getProductId(), item.getQuantity() != null ? item.getQuantity() : 0, Integer::sum);
            }
        }
        shift.setTopSellingProductIds(productQty.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5).map(Map.Entry::getKey).collect(Collectors.toList()));

        return ShiftReportMapper.toDto(shiftReportRepository.save(shift));
    }

    @Override
    public ShiftReportDto getShiftReportById(Long id) throws Exception
    {
        return ShiftReportMapper.toDto(shiftReportRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("ShiftReport", id)));
    }

    @Override
    public List<ShiftReportDto> getAllShiftReports()
    {
        return shiftReportRepository.findAll().stream().map(ShiftReportMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<ShiftReportDto> getShiftReportsByStoreId(Long storeId) throws Exception
    {
        storeAccessService.requireStoreAccess(storeId);
        return shiftReportRepository.findByStoreId(storeId).stream().map(ShiftReportMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<ShiftReportDto> getShiftReportsByCashierEntity_Id(Long cashierId)
    {
        return shiftReportRepository.findByCashierId(cashierId).stream().map(ShiftReportMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public ShiftReportDto getCurrentShiftProgress(Long cashierId) throws UserException
    {
        Long id = cashierId != null ? cashierId : requireCurrentCashierId();
        return ShiftReportMapper.toDto(requireOpenShift(id));
    }

    @Override
    public ShiftReportDto getShiftByCashierEntityAndDate(Long cashierId, LocalDateTime date) throws Exception
    {
        LocalDateTime start = date.toLocalDate().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return ShiftReportMapper.toDto(shiftReportRepository.findByCashierIdAndShiftStartBetween(cashierId, start, end)
                .orElseThrow(() -> ExceptionMessages.notFound("ShiftReport", cashierId, "for date " + date)));
    }

    @Override
    public void deleteShiftReport(Long id) throws Exception
    {
        ShiftReportEntity shift = shiftReportRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("ShiftReport", id, "delete"));
        shiftReportRepository.delete(shift);
    }

    @Override
    public void deleteAllShiftReports()
    {
        shiftReportRepository.deleteAll();
    }

    private Long requireCurrentCashierId() throws UserException
    {
        UserDto currentUser = userService.getCurrentUser();
        if(currentUser.getId() == null)
        {
            throw new UserException("Authenticated user has no userId");
        }
        return currentUser.getId();
    }

    private ShiftReportEntity requireOpenShift(Long cashierId) throws UserException
    {
        return shiftReportRepository
                .findTopByCashierIdAndShiftEndIsNullOrderByShiftStartDesc(cashierId)
                .orElseThrow(() -> new UserException("No open shift for cashierId=" + cashierId));
    }

    private List<PaymentSummaryEntity> buildPaymentSummaries(List<OrderEntity> orders)
    {
        Map<PaymentType, List<OrderEntity>> byType = orders.stream()
                .filter(o -> o.getPaymentType() != null)
                .collect(Collectors.groupingBy(OrderEntity::getPaymentType));
        double total = orders.stream().mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0).sum();
        List<PaymentSummaryEntity> summaries = new ArrayList<>();
        for(Map.Entry<PaymentType, List<OrderEntity>> e : byType.entrySet())
        {
            double amount = e.getValue().stream().mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0).sum();
            PaymentSummaryEntity summary = new PaymentSummaryEntity();
            summary.setType(e.getKey());
            summary.setTotalAmount(amount);
            summary.setTransactionCount(e.getValue().size());
            summary.setPercentage(total > 0 ? (amount / total) * 100.0 : 0.0);
            summaries.add(summary);
        }
        return summaries;
    }
}
