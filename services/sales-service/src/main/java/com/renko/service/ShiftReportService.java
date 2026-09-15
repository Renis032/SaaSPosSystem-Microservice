package com.renko.service;

import com.renko.exceptions.UserException;
import com.renko.payload.dto.ShiftReportDto;

import java.time.LocalDateTime;
import java.util.List;

public interface ShiftReportService
{
    ShiftReportDto startShift() throws Exception;

    ShiftReportDto startShift(Long branchId) throws Exception;

    ShiftReportDto endShift(Long shiftReportId,
                            LocalDateTime shiftEnd) throws Exception;

    ShiftReportDto getShiftReportById(Long id) throws Exception;

    List<ShiftReportDto> getAllShiftReports();

    List<ShiftReportDto> getShiftReportsByStoreId(Long storeId) throws Exception;

    List<ShiftReportDto> getShiftReportsByCashierEntity_Id(Long cashierId);

    ShiftReportDto getCurrentShiftProgress(Long cashierId) throws UserException;

    ShiftReportDto getShiftByCashierEntityAndDate(Long cashierId, LocalDateTime date) throws Exception;

    void deleteShiftReport(Long id) throws Exception;

    void deleteAllShiftReports();
}
