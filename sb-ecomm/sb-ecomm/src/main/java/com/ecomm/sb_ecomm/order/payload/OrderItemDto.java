package com.ecomm.sb_ecomm.order.payload;

import com.ecomm.sb_ecomm.product.payload.ProductDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDto {

    private Long orderItemId;
    private ProductDto product;
    private int quantity;
    private double price;
    private double discount;
}
