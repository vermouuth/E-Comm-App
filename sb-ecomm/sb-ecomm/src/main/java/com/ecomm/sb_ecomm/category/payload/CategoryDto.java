package com.ecomm.sb_ecomm.category.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDto {

    private Long id;
    @NotBlank(message = "categoryName is required")
    @Size(min = 5, max = 100, message = "categoryName must be 5-100 characters")
    private String categoryName;
}
