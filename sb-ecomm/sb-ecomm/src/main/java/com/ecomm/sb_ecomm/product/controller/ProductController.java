package com.ecomm.sb_ecomm.product.controller;

import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;

import com.ecomm.sb_ecomm.config.AppConstantPaginationValues;
import com.ecomm.sb_ecomm.config.OpenApiConfig;
import com.ecomm.sb_ecomm.product.payload.ProductDto;
import com.ecomm.sb_ecomm.product.payload.ProductResponse;
import com.ecomm.sb_ecomm.product.services.ProductService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "Products", description = "Browse and manage products.")
@Validated
@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService)
    {
        this.productService = productService;
    }


    @Operation(summary = "List products", description = "Public, paginated list of products.")
    @ApiResponse(responseCode = "200", description = "Paginated products",
            content = @Content(schema = @Schema(implementation = ProductResponse.class)))
    @GetMapping("/public/products")
    public ResponseEntity<?> getProducts(@Parameter(description = "Number of items per page") @Min(value = 1, message = "pageSize must be at least 1") @Max(value = 100, message = "pageSize must be at most 100") @RequestParam(name = "pageSize", defaultValue = AppConstantPaginationValues.PAGE_SIZE) Integer pageSize,
                                         @Parameter(description = "1-based page number") @Min(value = 1, message = "pageNumber must be at least 1") @RequestParam(name = "pageNumber" , defaultValue = AppConstantPaginationValues.PAGE_NUMBER) Integer pageNumber ,
                                         @Parameter(description = "Field to sort by") @RequestParam(name = "sortBy", defaultValue = AppConstantPaginationValues.SORT_PRODUCT_BY) String sortBy,
                                         @Parameter(description = "Sort direction: asc or desc") @RequestParam(name = "sortOrder", defaultValue = AppConstantPaginationValues.SORT_DIR) String sortOrder){
        ProductResponse productResponse = this.productService.getAllProducts(pageSize,pageNumber,sortBy,sortOrder);

        return new ResponseEntity<>(productResponse,HttpStatus.OK);
    }

    @Operation(summary = "Get a product by id", description = "Public endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found",
                    content = @Content(schema = @Schema(implementation = ProductDto.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @GetMapping("/public/products/{productId}")
    public ResponseEntity<?> getProductById(@Parameter(description = "Product id") @Positive(message = "productId must be a positive number") @PathVariable("productId") Long productId){
        return new ResponseEntity<>(this.productService.getProductById(productId), HttpStatus.OK);
    }

    @Operation(summary = "List products in a category", description = "Public, paginated list of products within a category.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated products",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Category not found",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @GetMapping("/public/categories/{categoryId}/products")
    public ResponseEntity<?> getProductsByCategory(@Parameter(description = "Number of items per page") @Min(value = 1, message = "pageSize must be at least 1") @Max(value = 100, message = "pageSize must be at most 100") @RequestParam(name = "pageSize", defaultValue = AppConstantPaginationValues.PAGE_SIZE) Integer pageSize,
                                                   @Parameter(description = "1-based page number") @Min(value = 1, message = "pageNumber must be at least 1") @RequestParam(name = "pageNumber" , defaultValue = AppConstantPaginationValues.PAGE_NUMBER) Integer pageNumber ,
                                                   @Parameter(description = "Field to sort by") @RequestParam(name = "sortBy", defaultValue = AppConstantPaginationValues.SORT_PRODUCT_BY) String sortBy,
                                                   @Parameter(description = "Sort direction: asc or desc") @RequestParam(name = "sortOrder", defaultValue = AppConstantPaginationValues.SORT_DIR) String sortOrder,
                                                   @Parameter(description = "Category id") @Positive(message = "categoryId must be a positive number") @PathVariable Long categoryId){
        ProductResponse productResponse = this.productService.getProductsByCategory(categoryId,pageSize,pageNumber,sortBy,sortOrder);
        return new ResponseEntity<>(productResponse,HttpStatus.OK);
    }

    @Operation(summary = "Search products by keyword", description = "Public, paginated product search.")
    @ApiResponse(responseCode = "200", description = "Paginated products",
            content = @Content(schema = @Schema(implementation = ProductResponse.class)))
    @GetMapping("/public/products/keyword/{keyword}")
    public ResponseEntity<?> getProductsByKeyword(@Parameter(description = "Number of items per page") @Min(value = 1, message = "pageSize must be at least 1") @Max(value = 100, message = "pageSize must be at most 100") @RequestParam(name = "pageSize", defaultValue = AppConstantPaginationValues.PAGE_SIZE) Integer pageSize,
                                                  @Parameter(description = "1-based page number") @Min(value = 1, message = "pageNumber must be at least 1") @RequestParam(name = "pageNumber" , defaultValue = AppConstantPaginationValues.PAGE_NUMBER) Integer pageNumber ,
                                                  @Parameter(description = "Field to sort by") @RequestParam(name = "sortBy", defaultValue = AppConstantPaginationValues.SORT_PRODUCT_BY) String sortBy,
                                                  @Parameter(description = "Sort direction: asc or desc") @RequestParam(name = "sortOrder", defaultValue = AppConstantPaginationValues.SORT_DIR) String sortOrder,
                                                  @Parameter(description = "Search keyword") @NotBlank(message = "keyword is required") @PathVariable String keyword){
        ProductResponse productResponse = this.productService.getProductsByKeyword(keyword,pageSize,pageNumber,sortBy,sortOrder);
        return ResponseEntity.ok(productResponse);
    }

    @Operation(summary = "Count all products", description = "Requires ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Total product count",
            content = @Content(schema = @Schema(implementation = Long.class)))
    @SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
    @GetMapping("/admin/products/count")
    public ResponseEntity<?> getProductsCount(){
        return new ResponseEntity<>(this.productService.countAllProducts(),HttpStatus.OK);
    }

    @Operation(summary = "Create a product in a category", description = "Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created",
                    content = @Content(schema = @Schema(implementation = ProductDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Category not found",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
    @PostMapping("/admin/categories/{categoryId}/product")
    public ResponseEntity<?> addProduct(@Parameter(description = "Category id") @Positive(message = "categoryId must be a positive number") @PathVariable Long categoryId,@Valid @RequestBody ProductDto productDto){
        return  new ResponseEntity<>(this.productService.createProduct(categoryId,productDto), HttpStatus.CREATED);
    }

    @Operation(summary = "Update a product", description = "Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated",
                    content = @Content(schema = @Schema(implementation = ProductDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
    @PutMapping("/admin/products/{productId}")
    public ResponseEntity<?> updateProduct(@Parameter(description = "Product id") @Positive(message = "productId must be a positive number") @PathVariable("productId")Long productId ,@Valid @RequestBody ProductDto productDto){
        return new ResponseEntity<>(this.productService.updateProduct(productId , productDto),HttpStatus.OK);
    }

    @Operation(summary = "Update a product's image", description = "Requires ADMIN role. Accepts a multipart/form-data upload.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image updated",
                    content = @Content(schema = @Schema(implementation = ProductDto.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
    @PutMapping(value = "/admin/products/{productId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateImage(@Parameter(description = "Product id") @Positive(message = "productId must be a positive number") @PathVariable("productId") Long productId,
                                          @Parameter(description = "Image file to upload") @RequestParam MultipartFile image) throws IOException {
        ProductDto productDto = this.productService.updateProductImage(productId,image);
        return new ResponseEntity<>(productDto,HttpStatus.OK);
    }

    @Operation(summary = "Delete a product", description = "Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product deleted",
                    content = @Content(schema = @Schema(implementation = ProductDto.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
    @DeleteMapping("/admin/products/{productId}")
    public ResponseEntity<?> deleteProduct(@Parameter(description = "Product id") @Positive(message = "productId must be a positive number") @PathVariable("productId") Long productId){
        return new ResponseEntity<>(this.productService.deleteProduct(productId),HttpStatus.OK);
    }
}
