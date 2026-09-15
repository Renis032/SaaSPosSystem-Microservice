package com.renko.entities;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Email;
import lombok.*;

@Getter
@Setter
@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreContactEntity
{
    @Email(message = "Invalid email")
    public String email;

    public String phone;
    public String address;
}
