package com.ecomm.sb_ecomm.payload.responses;

import com.ecomm.sb_ecomm.payload.dto.ProductDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class ProductResponse {
    List<ProductDto> products;
    private Integer pageNumber;
    private Integer pageSize;
    private Integer totalPages;
    private Long totalElements;
    private Boolean isLastPage;
}
