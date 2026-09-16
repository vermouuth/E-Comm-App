package com.ecomm.sb_ecomm.order.model;

import com.ecomm.sb_ecomm.common.persistence.BaseEntity;
import com.ecomm.sb_ecomm.product.model.Product;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OrderItem extends BaseEntity {

    private double discount;
    private double productPrice;
    private int quantity;

    @ManyToOne
    private Product product;

    @ManyToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Orders orders;
}
