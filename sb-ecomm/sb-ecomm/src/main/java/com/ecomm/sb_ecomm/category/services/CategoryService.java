package com.ecomm.sb_ecomm.category.services;

import com.ecomm.sb_ecomm.category.payload.CategoryDto;
import com.ecomm.sb_ecomm.category.payload.CategoryResponse;

public interface CategoryService {

    CategoryDto getCategoryById(Long id);
    CategoryDto updateCategory(Long categoryId , CategoryDto categoryDto);
    CategoryDto addCategory(CategoryDto categoryDto);

    String deleteCategory(Long id);

    CategoryResponse getCategories(Integer pageNumber, Integer pageSize , String sortBy , String sortDirection);
}
