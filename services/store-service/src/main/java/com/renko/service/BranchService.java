package com.renko.service;

import com.renko.exceptions.UserException;
import com.renko.payload.dto.BranchDto;
import com.renko.payload.dto.updates.BranchUpdateDto;

import java.util.List;

public interface BranchService
{
    BranchDto createBranch(BranchDto branchDto) throws UserException;
    BranchDto updateBranch(Long id, BranchUpdateDto branchDto) throws Exception;
    BranchDto getBranchById(Long id) throws Exception;
    List<BranchDto> getAllBranches();
    void deleteBranch(Long id) throws Exception;
    void deleteAllBranches();
    List<BranchDto> getAllBranchesByStoreId(Long id) throws Exception;
}
