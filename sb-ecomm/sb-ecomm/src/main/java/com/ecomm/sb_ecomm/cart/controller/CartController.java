package com.ecomm.sb_ecomm.cart.controller;

import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;

import com.ecomm.sb_ecomm.cart.payload.CartDto;
import com.ecomm.sb_ecomm.cart.services.CartServices;
import com.ecomm.sb_ecomm.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Cart", description = "Manage shopping carts. All endpoints require authentication.")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@Validated
@RestController
@RequestMapping("/api")
public class CartController {

    private final CartServices cartServices;

    public CartController(CartServices cartServices) {
        this.cartServices = cartServices;
    }

    @Operation(summary = "Add a product to the current user's cart")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product added to cart",
                    content = @Content(schema = @Schema(implementation = CartDto.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @PostMapping("/user/carts/{productId}/quantity/{quantity}")
    public ResponseEntity<?> addProductToCart(@Parameter(description = "Product id") @Positive(message = "productId must be a positive number") @PathVariable("productId") Long productId,
                                               @Parameter(description = "Quantity to add") @Positive(message = "quantity must be greater than 0") @PathVariable("quantity") int quantity){
        CartDto cartDto = this.cartServices.addProductToCart(productId,quantity);
        return new ResponseEntity<>(cartDto, HttpStatus.CREATED);
    }

    @Operation(summary = "Set a product's quantity in the current user's cart",
            description = "Overwrites the quantity (does not add to it). A quantity of 0 removes the product.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantity updated",
                    content = @Content(schema = @Schema(implementation = CartDto.class))),
            @ApiResponse(responseCode = "400", description = "Negative quantity, or more than the available stock"),
            @ApiResponse(responseCode = "404", description = "Product not found in cart",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @PutMapping("/user/carts/{productId}/quantity/{quantity}")
    public ResponseEntity<?> updateUserCartQuantity(@Parameter(description = "Product id") @Positive(message = "productId must be a positive number") @PathVariable("productId") Long productId,
                                                    @Parameter(description = "New quantity") @PositiveOrZero(message = "quantity must be 0 or more") @PathVariable("quantity") int quantity){
        return new ResponseEntity<>(this.cartServices.updateProductQuantity(productId, quantity), HttpStatus.OK);
    }

    @Operation(summary = "List all carts", description = "Requires ADMIN role.")
    @ApiResponse(responseCode = "200", description = "All carts",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CartDto.class))))
    @GetMapping("/admin/carts")
    public ResponseEntity<?> getCarts(){
        List<CartDto> cartDtoList = this.cartServices.getCarts();
        return ResponseEntity.ok().body(cartDtoList);
    }


    @Operation(summary = "Get the current user's cart")
    @ApiResponse(responseCode = "200", description = "Current user's cart",
            content = @Content(schema = @Schema(implementation = CartDto.class)))
    @GetMapping("/user/carts/users/cart")
    public ResponseEntity<?> getUserCart(){
        CartDto userCart = this.cartServices.getUserCart();
        return ResponseEntity.ok().body(userCart);
    }

    @Operation(summary = "Get a user's cart by email", description = "Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart found",
                    content = @Content(schema = @Schema(implementation = CartDto.class))),
            @ApiResponse(responseCode = "404", description = "Cart not found",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @GetMapping("/admin/carts/users/cart/{userEmail}")
    public ResponseEntity<?> getUserCartByEmail(@Parameter(description = "User's email") @Email(message = "userEmail must be a valid email") @PathVariable("userEmail") String userEmail)
    {
        CartDto cartDto = this.cartServices.getCartByEmail(userEmail);
        return ResponseEntity.ok(cartDto);
    }

    @Operation(summary = "Update a product's quantity in a cart", description = "Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantity updated",
                    content = @Content(schema = @Schema(implementation = CartDto.class))),
            @ApiResponse(responseCode = "404", description = "Cart or product not found",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @PutMapping("/admin/cart/products/{productId}/quantity/{quantity}")
    public ResponseEntity<?> updateProductQuantity(@Parameter(description = "Product id") @Positive(message = "productId must be a positive number") @PathVariable("productId") Long productId,
                                                     @Parameter(description = "New quantity") @PositiveOrZero(message = "quantity must be 0 or more") @PathVariable("quantity") int quantity){
        CartDto updatedCart = this.cartServices.updateProductQuantity(productId , quantity);
        return new ResponseEntity<>(updatedCart, HttpStatus.OK);
    }

    @Operation(summary = "Remove a product from a cart", description = "Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Confirmation message",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Cart or product not found",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @DeleteMapping("/admin/carts/{cartId}/product/{productId}")
    public String deleteProductFromCart(@Parameter(description = "Cart id") @Positive(message = "cartId must be a positive number") @PathVariable("cartId") Long cartId,
                                         @Parameter(description = "Product id") @Positive(message = "productId must be a positive number") @PathVariable Long productId){
        return this.cartServices.deleteProductFromCart(cartId, productId);
    }

    @Operation(summary = "Remove a product from the current user's cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product removed",
                    content = @Content(schema = @Schema(implementation = CartDto.class))),
            @ApiResponse(responseCode = "404", description = "Product not found in cart",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @DeleteMapping("/user/cart/product/{productId}")
    public ResponseEntity<?> deleteProductFromCurrentUser(@Parameter(description = "Product id") @Positive(message = "productId must be a positive number") @PathVariable("productId") Long productId){
        CartDto cartDto = this.cartServices.deleteProductFromCurrentCart(productId);
        return new ResponseEntity<>(cartDto, HttpStatus.OK);
    }
}
