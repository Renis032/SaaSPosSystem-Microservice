package com.renko.entities;

import com.renko.domain.StoreStatus;
import com.renko.payload.dto.StoreDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StoreEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String brandName;

    @Column(name = "store_admin_id")
    private Long storeAdminId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String description;
    private String storeType;
    private StoreStatus status;

    @Embedded
    private StoreContactEntity contact;

    @PrePersist
    protected void onCreate()
    {
        createdAt = LocalDateTime.now();
        status = StoreStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate()
    {
        updatedAt = LocalDateTime.now();
    }

    public void setFromDto(StoreDto storeDto)
    {
        this.id = storeDto.getId();
        this.brandName = storeDto.getBrandName();
        this.description = storeDto.getDescription();
        this.storeType = storeDto.getStoreType();
        this.createdAt = storeDto.getCreatedAt();
        this.updatedAt = storeDto.getUpdatedAt();
        if(storeDto.getStoreAdminId() != null)
        {
            this.storeAdminId = storeDto.getStoreAdminId();
        }
        if(storeDto.getStatus() != null)
        {
            this.status = storeDto.getStatus();
        }
        if(storeDto.getContact() != null)
        {
            this.contact = StoreContactEntity.builder()
                    .email(storeDto.getContact().getEmail())
                    .phone(storeDto.getContact().getPhone())
                    .address(storeDto.getContact().getAddress())
                    .build();
        }
    }
}
