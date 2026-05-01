package com.ecomm.sb_ecomm.controllers;

import com.ecomm.sb_ecomm.payload.dto.CartDto;
import com.ecomm.sb_ecomm.services.CartServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CartController {

    private final CartServices cartServices;

    public CartController(CartServices cartServices) {
        this.cartServices = cartServices;
    }

    @PostMapping("/user/carts/{productId}/quantity/{quantity}")
    public ResponseEntity<?> addProductToCart(@PathVariable("productId") Long productId, @PathVariable("quantity") int quantity){
        CartDto cartDto = this.cartServices.addProductToCart(productId,quantity);
        return new ResponseEntity<>(cartDto, HttpStatus.CREATED);
    }

    @GetMapping("/admin/carts")
    public ResponseEntity<?> getCarts(){
        List<CartDto> cartDtoList = this.cartServices.getCarts();
        return ResponseEntity.ok().body(cartDtoList);
    }


    @GetMapping("/user/carts/users/cart")
    public ResponseEntity<?> getUserCart(){
        CartDto userCart = this.cartServices.getUserCart();
        return ResponseEntity.ok().body(userCart);
    }

    @GetMapping("/admin/carts/users/cart/{userEmail}")
    public ResponseEntity<?> getUserCartByEmail(@PathVariable("userEmail") String userEmail)
    {
        CartDto cartDto = this.cartServices.getCartByEmail(userEmail);
        return ResponseEntity.ok(cartDto);
    }

        @PutMapping("/admin/cart/products/{productId}/quantity/{quantity}")
    public ResponseEntity<?> updateProductQuantity(@PathVariable("productId") Long productId, @PathVariable("quantity") int quantity){
        CartDto updatedCart = this.cartServices.updateProductQuantity(productId , quantity);
        return new ResponseEntity<>(updatedCart, HttpStatus.OK);
    }

    @DeleteMapping("/admin/carts/{cartId}/product/{productId}")
    public String deleteProductFromCart(@PathVariable("cartId") Long cartId, @PathVariable Long productId){
        return this.cartServices.deleteProductFromCart(cartId, productId);
    }

        @DeleteMapping("/user/cart/product/{productId}")
    public ResponseEntity<?> deleteProductFromCurrentUser(@PathVariable("productId") Long productId){
        CartDto cartDto = this.cartServices.deleteProductFromCurrentCart(productId);
        return new ResponseEntity<>(cartDto, HttpStatus.OK);
    }
}
