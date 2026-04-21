package com.ecomm.sb_ecomm.payload.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartDto {
    Long cartId;
    List<ProductDto> products;
    private double totalPrice;
}
