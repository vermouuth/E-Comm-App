package com.ecomm.sb_ecomm.models;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@NoArgsConstructor
@Data
@SuperBuilder
@Table(name = "roles")
@EqualsAndHashCode(callSuper = true)
public class Role  extends BaseEntity{

    @ToString.Exclude
    @Column(length = 20,name = "role_name")
    @Enumerated(EnumType.STRING)
    private AppRole roleName;

    public Role(AppRole roleName)
    {
        this.roleName = roleName;
    }



}
