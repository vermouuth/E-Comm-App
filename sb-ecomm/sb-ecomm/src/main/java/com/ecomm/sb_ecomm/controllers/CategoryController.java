package com.ecomm.sb_ecomm.controllers;

import com.ecomm.sb_ecomm.config.AppConstantPaginationValues;
import com.ecomm.sb_ecomm.payload.dto.CategoryDto;
import com.ecomm.sb_ecomm.services.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService)
    {
        this.categoryService = categoryService;
    }


    @PostMapping("/admin/categories")
    public ResponseEntity<CategoryDto> addCategory(@Valid @RequestBody CategoryDto categoryDto)
    {
        CategoryDto catDto = this.categoryService.addCategory(categoryDto);
        return new  ResponseEntity<>(catDto,HttpStatus.CREATED);
    }

    @GetMapping("/public/categories")
    public ResponseEntity<?> getCategories(
            @RequestParam(name = "pageNumber" , defaultValue = AppConstantPaginationValues.PAGE_NUMBER , required = false) Integer pageNumber,
            @RequestParam(name = "pageSize" , defaultValue = AppConstantPaginationValues.PAGE_SIZE , required = false) Integer pageSize,
            @RequestParam(name = "sortBy" , defaultValue = AppConstantPaginationValues.SORT_CATEGORY_BY , required = false) String sortBy,
            @RequestParam(name = "sortOrder" , defaultValue = AppConstantPaginationValues.SORT_DIR , required = false) String sortOrder){

        return ResponseEntity.ok(this.categoryService.getCategories(pageNumber, pageSize, sortBy, sortOrder));
    }

    @GetMapping("/public/categories/{categoryId}")
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable Long categoryId){
        CategoryDto foundedCategory = this.categoryService.getCategoryById(categoryId);
        return new ResponseEntity<>(foundedCategory,HttpStatus.OK);
    }

    @PutMapping("/admin/categories/{categoryId}")
    public ResponseEntity<CategoryDto> updateCategory(@PathVariable Long categoryId , @RequestBody CategoryDto categoryDto) {

        CategoryDto updatedCategory = this.categoryService.updateCategory(categoryId ,  categoryDto);
        return new ResponseEntity<>(updatedCategory,HttpStatus.OK);

    }

    @DeleteMapping("/admin/categories/{categoryId}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long categoryId) {
       String deletedCategory = this.categoryService.deleteCategory(categoryId);
       return new ResponseEntity<>(deletedCategory,HttpStatus.OK);
    }






}
