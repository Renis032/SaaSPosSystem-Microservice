package com.renko.service.impl;

import com.renko.entities.BranchEntity;
import com.renko.entities.StoreEntity;
import com.renko.exceptions.ExceptionMessages;
import com.renko.exceptions.UserException;
import com.renko.mapper.BranchMapper;
import com.renko.payload.dto.BranchDto;
import com.renko.payload.dto.UserDto;
import com.renko.payload.dto.updates.BranchUpdateDto;
import com.renko.repository.BranchRepository;
import com.renko.repository.StoreRepository;
import com.renko.service.BranchService;
import com.renko.service.StoreAccessService;
import com.renko.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BranchServiceImpl implements BranchService
{
    private final BranchRepository branchRepository;
    private final StoreRepository storeRepository;
    private final UserService userService;
    private final StoreAccessService storeAccessService;

    @Override
    public BranchDto createBranch(BranchDto branchDto) throws UserException
    {
        UserDto currentUser = userService.getCurrentUser();
        StoreEntity storeEntity = resolveStoreForCreate(branchDto, currentUser);
        BranchEntity branch = BranchMapper.toEntity(branchDto, storeEntity.getId());
        if(branchDto.getManagerId() != null)
        {
            branch.setManagerId(branchDto.getManagerId());
        }
        return BranchMapper.toDto(branchRepository.save(branch));
    }

    @Override
    public BranchDto updateBranch(Long id, BranchUpdateDto branchDto) throws Exception
    {
        BranchEntity existingBranch = branchRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Branch", id, "update"));
        if(branchDto.getStoreId() != null)
        {
            storeRepository.findById(branchDto.getStoreId())
                    .orElseThrow(() -> ExceptionMessages.notFound("Store", branchDto.getStoreId(), "update branchId=" + id));
            existingBranch.setStoreId(branchDto.getStoreId());
        }
        existingBranch.updateFrom(branchDto);
        return BranchMapper.toDto(branchRepository.save(existingBranch));
    }

    @Override
    public BranchDto getBranchById(Long id) throws Exception
    {
        return BranchMapper.toDto(branchRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Branch", id)));
    }

    @Override
    public List<BranchDto> getAllBranches()
    {
        return branchRepository.findAll().stream().map(BranchMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<BranchDto> getAllBranchesByStoreId(Long storeId) throws Exception
    {
        storeAccessService.requireStoreAccess(storeId);
        return branchRepository.findByStoreId(storeId).stream().map(BranchMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public void deleteBranch(Long id) throws Exception
    {
        BranchEntity branch = branchRepository.findById(id)
                .orElseThrow(() -> ExceptionMessages.notFound("Branch", id, "delete"));
        branchRepository.delete(branch);
    }

    @Override
    public void deleteAllBranches()
    {
        branchRepository.deleteAll();
    }

    private StoreEntity resolveStoreForCreate(BranchDto branchDto, UserDto currentUser) throws UserException
    {
        Long storeId = branchDto.getStoreId() != null ? branchDto.getStoreId() : currentUser.getStoreId();
        if(storeId == null)
        {
            throw ExceptionMessages.required("storeId", "storeId is required to create a branch");
        }
        storeAccessService.requireStoreAccess(storeId);
        return storeRepository.findById(storeId)
                .orElseThrow(() -> ExceptionMessages.notFound("Store", storeId, "create branch"));
    }
}
