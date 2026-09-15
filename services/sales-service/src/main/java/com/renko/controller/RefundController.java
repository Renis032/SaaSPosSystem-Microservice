package com.renko.controller;

import com.renko.payload.dto.RefundDto;
import com.renko.payload.response.ApiResponse;
import com.renko.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/refunds")
public class RefundController
{
    private final RefundService refundService;

    @PostMapping
    public ResponseEntity<RefundDto> createRefund(@RequestBody RefundDto refundDto) throws Exception
    {
        return ResponseEntity.ok(refundService.createRefund(refundDto));
    }

    @GetMapping
    public ResponseEntity<List<RefundDto>> getAllRefunds()
    {
        return ResponseEntity.ok(refundService.getAllRefunds());
    }

    @GetMapping("/cashier/{cashierId}")
    public ResponseEntity<List<RefundDto>> getRefundsByCashier(@PathVariable Long cashierId)
    {
        return ResponseEntity.ok(refundService.getRefundsByCashier(cashierId));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<RefundDto>> getRefundsByStore(@PathVariable Long storeId) throws Exception
    {
        return ResponseEntity.ok(refundService.getRefundsByStore(storeId));
    }

    @GetMapping("/shift/{shiftId}")
    public ResponseEntity<List<RefundDto>> getRefundsByShift(@PathVariable Long shiftId)
    {
        return ResponseEntity.ok(refundService.getRefundByShiftReport(shiftId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RefundDto> getRefundById(@PathVariable Long id) throws Exception
    {
        return ResponseEntity.ok(refundService.getRefundById(id));
    }

    @GetMapping("/cashier/{cashierId}/range")
    public ResponseEntity<List<RefundDto>> getRefundByCashierAndDateRange(@PathVariable Long cashierId,
                                                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end)
    {
        return ResponseEntity.ok(refundService.getRefundByCashierAndDateRange(cashierId, start, end));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteRefund(@PathVariable Long id)
    {
        refundService.deleteRefund(id);

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Successfully deleted refund");

        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse> deleteAllRefunds()
    {
        refundService.deleteAllRefunds();

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("All refunds deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }
}
