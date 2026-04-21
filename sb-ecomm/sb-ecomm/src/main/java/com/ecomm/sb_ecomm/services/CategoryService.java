package com.ecomm.sb_ecomm.services;

import com.ecomm.sb_ecomm.payload.dto.CategoryDto;
import com.ecomm.sb_ecomm.payload.responses.CategoryResponse;

import java.util.List;

public interface CategoryService {

    CategoryDto getCategoryById(Long id);
    CategoryDto updateCategory(Long categoryId , CategoryDto categoryDto);
    CategoryDto addCategory(CategoryDto categoryDto);

    String deleteCategory(Long id);

    CategoryResponse getCategories(Integer pageNumber, Integer pageSize , String sortBy , String sortDirection);
}
