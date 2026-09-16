package com.ecomm.sb_ecomm.cart.payload;

import com.ecomm.sb_ecomm.product.payload.ProductDto;
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
