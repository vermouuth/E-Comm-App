package com.ecomm.sb_ecomm.product.payload;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDto {

    private Long productId;
    @NotBlank(message = "productName is required")
    @Size(min = 3, max = 160, message = "productName must be 3-160 characters")
    private String productName;
    @NotBlank(message = "description is required")
    @Size(min = 3, max = 400, message = "description must be 3-400 characters")
    private String description;
    @NotNull(message = "quantity is required")
    @Min(value = 0, message = "quantity must be 0 or more")
    private Integer quantity;
    @NotNull(message = "discount is required")
    @DecimalMin(value = "0.0", message = "discount must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "discount must be between 0 and 100")
    private Double discount;
    @NotNull(message = "price is required")
    @Positive(message = "price must be greater than 0")
    private Double price;
    private Double specialPrice;
    private String image;
}
