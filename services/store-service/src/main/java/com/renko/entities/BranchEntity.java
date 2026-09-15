package com.renko.entities;

import com.renko.payload.dto.updates.BranchUpdateDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class BranchEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;
    private String address;
    private String phone;
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> workdays;

    private LocalTime openTime;
    private LocalTime closeTime;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "manager_id")
    private Long managerId;

    @PrePersist
    protected void onCreate()
    {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate()
    {
        updatedAt = LocalDateTime.now();
    }

    public void updateFrom(BranchUpdateDto dto)
    {
        if(dto.getName() != null) this.name = dto.getName();
        if(dto.getAddress() != null) this.address = dto.getAddress();
        if(dto.getPhone() != null) this.phone = dto.getPhone();
        if(dto.getEmail() != null) this.email = dto.getEmail();
        if(dto.getWorkdays() != null) this.workdays = dto.getWorkdays();
        if(dto.getOpenTime() != null) this.openTime = dto.getOpenTime();
        if(dto.getCloseTime() != null) this.closeTime = dto.getCloseTime();
        this.updatedAt = LocalDateTime.now();
    }
}
