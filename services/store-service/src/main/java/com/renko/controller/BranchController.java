package com.renko.controller;

import com.renko.exceptions.UserException;
import com.renko.payload.dto.BranchDto;
import com.renko.payload.dto.updates.BranchUpdateDto;
import com.renko.payload.response.ApiResponse;
import com.renko.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/branches")
public class BranchController
{
    private final BranchService branchService;

    @PostMapping
    public ResponseEntity<BranchDto> createBranch(@RequestBody BranchDto branchDto) throws UserException
    {
        return ResponseEntity.ok(branchService.createBranch(branchDto));
    }

    @GetMapping
    public ResponseEntity<List<BranchDto>> getAllBranches()
    {
        return ResponseEntity.ok(branchService.getAllBranches());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BranchDto> updateBranch(@PathVariable Long id,
                                                  @RequestBody BranchUpdateDto branchDto) throws Exception
    {
        return ResponseEntity.ok(branchService.updateBranch(id, branchDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BranchDto> getBranchById(@PathVariable Long id) throws Exception
    {
        return ResponseEntity.ok(branchService.getBranchById(id));
    }

    @GetMapping("/stores/{id}")
    public ResponseEntity<List<BranchDto>> getAllBranchesByStoreId(@PathVariable Long id) throws Exception
    {
        return ResponseEntity.ok(branchService.getAllBranchesByStoreId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteBranch(@PathVariable Long id) throws Exception
    {
        branchService.deleteBranch(id);

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Branch deleted successfully");

        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse> deleteAllBranches()
    {
        branchService.deleteAllBranches();

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("All branches deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }
}
