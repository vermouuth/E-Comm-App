package com.ecomm.sb_ecomm.product.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
