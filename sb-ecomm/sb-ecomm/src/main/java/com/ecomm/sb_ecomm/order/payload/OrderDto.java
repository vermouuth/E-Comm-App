package com.ecomm.sb_ecomm.order.payload;

import com.ecomm.sb_ecomm.product.payload.ProductDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {

    private Long orderId;
    private String email;
    private Double totalAmount;
    private String orderStatus;
    private String street;
    private String state;
    private String city;
    private String zip;
    private String country;
    private List<ProductDto> productDtoList;
}
