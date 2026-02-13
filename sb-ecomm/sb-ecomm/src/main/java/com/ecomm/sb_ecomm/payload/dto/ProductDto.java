package com.ecomm.sb_ecomm.payload.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDto {

    private Long productId;
    private String productName;
    private String description;
    private Integer quantity;
    private Double discount;
    private Double price;
    private Double specialPrice;
    private String image;
}
