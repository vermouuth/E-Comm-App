package com.ecomm.sb_ecomm.services;

import com.ecomm.sb_ecomm.payload.dto.ProductDto;
import com.ecomm.sb_ecomm.payload.responses.ProductResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ProductService {

    ProductDto createProduct(Long categoryId,ProductDto productDto);
    ProductResponse getAllProducts(Integer pageSize, Integer pageNumber, String sortBy, String sortOrder);
    ProductDto deleteProduct(Long productId);
    ProductDto updateProduct(Long productId , ProductDto productDto);
    ProductResponse getProductsByCategory(Long productId ,  Integer pageSize, Integer pageNumber, String sortBy, String sortOrder);
    ProductResponse getProductsByKeyword(String keyword, Integer pageSize, Integer pageNumber, String sortBy, String sortOrder);
    Long countAllProducts();
    ProductDto updateProductImage(Long productId, MultipartFile image) throws IOException;
}
