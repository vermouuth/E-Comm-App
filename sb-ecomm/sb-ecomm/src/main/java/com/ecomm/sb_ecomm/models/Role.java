package com.ecomm.sb_ecomm.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "role_id")
    private int roleId;

    @ToString.Exclude
    @Column(length = 20,name = "role_name")
    @Enumerated(EnumType.STRING)
    private AppRole roleName;


    public Role(AppRole appRole) {
        this.roleName = appRole;
    }
}
