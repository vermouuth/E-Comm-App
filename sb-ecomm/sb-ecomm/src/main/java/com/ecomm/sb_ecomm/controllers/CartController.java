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

    @PostMapping("/carts/{productId}/quantity/{quantity}")
    public ResponseEntity<?> addProductToCart(@PathVariable("productId") Long productId, @PathVariable("quantity") int quantity){
        CartDto cartDto = this.cartServices.addProductToCart(productId,quantity);
        return new ResponseEntity<>(cartDto, HttpStatus.CREATED);
    }

    @GetMapping("/carts")
    public ResponseEntity<?> getCarts(){
        List<CartDto> cartDtoList = this.cartServices.getCarts();
        return ResponseEntity.ok().body(cartDtoList);
    }

    @GetMapping("carts/users/cart")
    public ResponseEntity<?> getUserCart(){
        CartDto userCart = this.cartServices.getUserCart();
        return ResponseEntity.ok().body(userCart);
    }

    @GetMapping("/carts/users/cart/{userEmail}")
    public ResponseEntity<?> getUserCart(@PathVariable("userEmail") String userEmail){
        return new ResponseEntity<>(this.cartServices.getCartByEmail(userEmail),HttpStatus.FOUND);
    }

    @PutMapping("/cart/products/{productId}/quantity/{quantity}")
    public ResponseEntity<?> updateProductQuantity(@PathVariable("productId") Long productId, @PathVariable("quantity") int quantity){
        CartDto updatedCart = this.cartServices.updateProductQuantity(productId , quantity);
        return new ResponseEntity<>(updatedCart, HttpStatus.CREATED);
    }

    @DeleteMapping("/carts/{cartId}/product/{productId}")
    public String deleteProductFromCart(@PathVariable("cartId") Long cartId, @PathVariable Long productId){
        return this.cartServices.deleteProductFromCart(cartId, productId);
    }
}
