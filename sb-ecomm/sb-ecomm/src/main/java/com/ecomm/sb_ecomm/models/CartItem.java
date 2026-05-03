package com.ecomm.sb_ecomm.models;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CartItem extends BaseEntity {

    @ManyToOne()
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @ManyToOne()
    @JoinColumn(name = "product_id")
    private Product product;

    private double discount;

    private double productPrice;

    private int quantity;

}
