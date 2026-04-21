package com.ecomm.sb_ecomm.controllers;

import com.ecomm.sb_ecomm.config.AppConstantPaginationValues;
import com.ecomm.sb_ecomm.payload.dto.ProductDto;
import com.ecomm.sb_ecomm.payload.responses.ProductResponse;
import com.ecomm.sb_ecomm.services.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService)
    {
        this.productService = productService;
    }


    @GetMapping("/public/products")
    public ResponseEntity<?> getProducts(@RequestParam(name = "pageSize", defaultValue = AppConstantPaginationValues.PAGE_SIZE) Integer pageSize,
                                         @RequestParam(name = "pageNumber" , defaultValue = AppConstantPaginationValues.PAGE_NUMBER) Integer pageNumber ,
                                         @RequestParam(name = "sortBy", defaultValue = AppConstantPaginationValues.SORT_PRODUCT_BY) String sortBy,
                                         @RequestParam(name = "sortOrder", defaultValue = AppConstantPaginationValues.SORT_DIR) String sortOrder){
        ProductResponse productResponse = this.productService.getAllProducts(pageSize,pageNumber,sortBy,sortOrder);

        return new ResponseEntity<>(productResponse,HttpStatus.FOUND);
    }

    @GetMapping("/public/categories/{categoryId}/products")
    public ResponseEntity<?> getProductsByCategory(@RequestParam(name = "pageSize", defaultValue = AppConstantPaginationValues.PAGE_SIZE) Integer pageSize,
                                                   @RequestParam(name = "pageNumber" , defaultValue = AppConstantPaginationValues.PAGE_NUMBER) Integer pageNumber ,
                                                   @RequestParam(name = "sortBy", defaultValue = AppConstantPaginationValues.SORT_PRODUCT_BY) String sortBy,
                                                   @RequestParam(name = "sortOrder", defaultValue = AppConstantPaginationValues.SORT_DIR) String sortOrder,
                                                   @PathVariable Long categoryId){
        ProductResponse productResponse = this.productService.getProductsByCategory(categoryId,pageSize,pageNumber,sortBy,sortOrder);
        return new ResponseEntity<>(productResponse,HttpStatus.FOUND);
    }

    @GetMapping("/public/products/keyword/{keyword}")
    public ResponseEntity<?> getProductsByKeyword(@RequestParam(name = "pageSize", defaultValue = AppConstantPaginationValues.PAGE_SIZE) Integer pageSize,
                                                  @RequestParam(name = "pageNumber" , defaultValue = AppConstantPaginationValues.PAGE_NUMBER) Integer pageNumber ,
                                                  @RequestParam(name = "sortBy", defaultValue = AppConstantPaginationValues.SORT_PRODUCT_BY) String sortBy,
                                                  @RequestParam(name = "sortOrder", defaultValue = AppConstantPaginationValues.SORT_DIR) String sortOrder,
                                                  @PathVariable String keyword){
        ProductResponse productResponse = this.productService.getProductsByKeyword(keyword,pageSize,pageNumber,sortBy,sortOrder);
        return ResponseEntity.ok(productResponse);
    }

    @GetMapping("/admin/products/count")
    public ResponseEntity<?> getProductsCount(){
        return new ResponseEntity<>(this.productService.countAllProducts(),HttpStatus.OK);
    }

    @PostMapping("/admin/categories/{categoryId}/product")
    public ResponseEntity<?> addProduct(@PathVariable Long categoryId,@Valid @RequestBody ProductDto productDto){
        return  new ResponseEntity<>(this.productService.createProduct(categoryId,productDto), HttpStatus.CREATED);
    }

    @PutMapping("/admin/products/{productId}")
    public ResponseEntity<?> updateProduct(@PathVariable("productId")Long productId ,@Valid @RequestBody ProductDto productDto){
        return new ResponseEntity<>(this.productService.updateProduct(productId , productDto),HttpStatus.OK);
    }

    @PutMapping("/admin/products/{productId}/image")
    public ResponseEntity<?> updateImage(@PathVariable("productId") Long productId, @RequestParam MultipartFile image) throws IOException {
        ProductDto productDto = this.productService.updateProductImage(productId,image);
        return new ResponseEntity<>(productDto,HttpStatus.OK);
    }

    @DeleteMapping("/admin/products/{productId}")
    public ResponseEntity<?> deleteProduct(@PathVariable("productId") Long productId){
        return new ResponseEntity<>(this.productService.deleteProduct(productId),HttpStatus.OK);
    }
}
