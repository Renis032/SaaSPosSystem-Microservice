package com.renko.service;

import com.renko.domain.UserRole;
import com.renko.exceptions.UserException;
import com.renko.payload.dto.UserDto;
import com.renko.service.impl.StoreAccessServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreAccessServiceImplTest
{
    @Mock
    private UserService userService;

    @InjectMocks
    private StoreAccessServiceImpl storeAccessService;

    @Test
    void ownerCanAccessOwnStore() throws Exception
    {
        when(userService.getCurrentUser()).thenReturn(user(1L, UserRole.OWNER, 10L));
        assertDoesNotThrow(() -> storeAccessService.requireStoreAccess(10L));
    }

    @Test
    void ownerCannotAccessOtherStore() throws Exception
    {
        when(userService.getCurrentUser()).thenReturn(user(1L, UserRole.OWNER, 10L));
        UserException ex = assertThrows(UserException.class, () -> storeAccessService.requireStoreAccess(99L));
        assertEquals("You do not have access to this store", ex.getMessage());
    }

    @Test
    void platformAdminCanAccessAnyStore() throws Exception
    {
        when(userService.getCurrentUser()).thenReturn(user(1L, UserRole.ADMIN, null));
        assertDoesNotThrow(() -> storeAccessService.requireStoreAccess(99L));
    }

    private static UserDto user(Long id, UserRole role, Long storeId)
    {
        UserDto dto = new UserDto();
        dto.setId(id);
        dto.setRole(role);
        dto.setStoreId(storeId);
        dto.setEmail("user@test.com");
        return dto;
    }
}
