package com.ecomm.sb_ecomm.category.controllers;

import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;

import com.ecomm.sb_ecomm.config.AppConstantPaginationValues;
import com.ecomm.sb_ecomm.category.payload.CategoryDto;
import com.ecomm.sb_ecomm.category.payload.CategoryResponse;
import com.ecomm.sb_ecomm.category.services.CategoryService;
import com.ecomm.sb_ecomm.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Categories", description = "Browse and manage product categories.")
@Validated
@RestController
@RequestMapping("/api")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService)
    {
        this.categoryService = categoryService;
    }


    @Operation(summary = "Create a category", description = "Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created",
                    content = @Content(schema = @Schema(implementation = CategoryDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not an ADMIN")
    })
    @SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
    @PostMapping("/admin/categories")
    public ResponseEntity<CategoryDto> addCategory(@Valid @RequestBody CategoryDto categoryDto)
    {
        CategoryDto catDto = this.categoryService.addCategory(categoryDto);
        return new  ResponseEntity<>(catDto,HttpStatus.CREATED);
    }

    @Operation(summary = "List categories", description = "Public, paginated list of categories.")
    @ApiResponse(responseCode = "200", description = "Paginated categories",
            content = @Content(schema = @Schema(implementation = CategoryResponse.class)))
    @GetMapping("/public/categories")
    public ResponseEntity<?> getCategories(
            @Parameter(description = "1-based page number") @Min(value = 1, message = "pageNumber must be at least 1") @RequestParam(name = "pageNumber" , defaultValue = AppConstantPaginationValues.PAGE_NUMBER , required = false) Integer pageNumber,
            @Parameter(description = "Number of items per page") @Min(value = 1, message = "pageSize must be at least 1") @Max(value = 100, message = "pageSize must be at most 100") @RequestParam(name = "pageSize" , defaultValue = AppConstantPaginationValues.PAGE_SIZE , required = false) Integer pageSize,
            @Parameter(description = "Field to sort by") @RequestParam(name = "sortBy" , defaultValue = AppConstantPaginationValues.SORT_CATEGORY_BY , required = false) String sortBy,
            @Parameter(description = "Sort direction: asc or desc") @RequestParam(name = "sortOrder" , defaultValue = AppConstantPaginationValues.SORT_DIR , required = false) String sortOrder){

        return ResponseEntity.ok(this.categoryService.getCategories(pageNumber, pageSize, sortBy, sortOrder));
    }

    @Operation(summary = "Get a category by id", description = "Public endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category found",
                    content = @Content(schema = @Schema(implementation = CategoryDto.class))),
            @ApiResponse(responseCode = "404", description = "Category not found",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @GetMapping("/public/categories/{categoryId}")
    public ResponseEntity<CategoryDto> getCategoryById(@Parameter(description = "Category id") @Positive(message = "categoryId must be a positive number") @PathVariable Long categoryId){
        CategoryDto foundedCategory = this.categoryService.getCategoryById(categoryId);
        return new ResponseEntity<>(foundedCategory,HttpStatus.OK);
    }

    @Operation(summary = "Update a category", description = "Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category updated",
                    content = @Content(schema = @Schema(implementation = CategoryDto.class))),
            @ApiResponse(responseCode = "404", description = "Category not found",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not an ADMIN")
    })
    @SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
    @PutMapping("/admin/categories/{categoryId}")
    public ResponseEntity<CategoryDto> updateCategory(@Parameter(description = "Category id") @Positive(message = "categoryId must be a positive number") @PathVariable Long categoryId , @Valid @RequestBody CategoryDto categoryDto) {

        CategoryDto updatedCategory = this.categoryService.updateCategory(categoryId ,  categoryDto);
        return new ResponseEntity<>(updatedCategory,HttpStatus.OK);

    }

    @Operation(summary = "Delete a category", description = "Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category deleted, confirmation message returned",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Category not found",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not an ADMIN")
    })
    @SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
    @DeleteMapping("/admin/categories/{categoryId}")
    public ResponseEntity<String> deleteCategory(@Parameter(description = "Category id") @Positive(message = "categoryId must be a positive number") @PathVariable Long categoryId) {
       String deletedCategory = this.categoryService.deleteCategory(categoryId);
       return new ResponseEntity<>(deletedCategory,HttpStatus.OK);
    }

}
