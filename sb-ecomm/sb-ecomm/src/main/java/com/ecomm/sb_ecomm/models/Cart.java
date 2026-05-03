package com.ecomm.sb_ecomm.models;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
@Table(name = "cart")
@EqualsAndHashCode(callSuper = true)
public class Cart extends BaseEntity {


    @OneToOne
    private Users uniqueUser;

    @OneToMany(mappedBy = "cart", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    private List<CartItem> cartItems = new ArrayList<>();

    @Column(nullable = false)
    private double totalPrice = 0.0;


}
