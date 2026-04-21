package com.ecomm.sb_ecomm.models;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @ManyToOne()
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @ManyToOne()
    @JoinColumn(name = "product_id")
    private Product product;

    public CartItem(Cart cart, Product product, double discount, double productPrice, int quantity) {
        this.cart = cart;
        this.product = product;
        this.discount = discount;
        this.productPrice = productPrice;
        this.quantity = quantity;
    }

    private double discount;

    private double productPrice;

    private int quantity;

}
