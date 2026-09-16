package com.ecomm.sb_ecomm.cart.payload;

import com.ecomm.sb_ecomm.product.payload.ProductDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItemDto {
    Long cartItemId;
    CartDto cart;
    ProductDto product;
    double discount;
    double productPrice;
    int quantity;
}
