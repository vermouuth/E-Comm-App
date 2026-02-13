package com.ecomm.sb_ecomm.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "products")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @NotBlank
    @Size(min = 3 , max = 160 , message = "Product name must contain at least 3 letters")
    private String productName;

    @NotBlank
    @Size(min = 3 , max = 400 , message = "Product description must contain at least 3 letters")
    private String description;

    private String image;


    @NotNull
    @Min(1)
    @Max(100)
    private Double discount;

    @NotNull
    @Min(1)
    private Double price;


    private Double specialPrice;

    @NotNull
    @Min(value = 1, message = "At least 1 piece")
    @Column(nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Keep specialPrice consistent automatically
    @PrePersist
    @PreUpdate
    private void syncSpecialPrice() {
        this.specialPrice = recalculateSpecialPrice();
    }

    public Double recalculateSpecialPrice() {
        Double totalPrice = getPrice() - (getPrice() * (getDiscount() / 100));

        return totalPrice * getQuantity();
    }
}
