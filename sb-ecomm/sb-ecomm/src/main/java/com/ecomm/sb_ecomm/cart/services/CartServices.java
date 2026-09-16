package com.ecomm.sb_ecomm.cart.services;

import com.ecomm.sb_ecomm.cart.payload.CartDto;

import java.util.List;

public interface CartServices {
    CartDto addProductToCart(Long productId, int quantity);

    List<CartDto> getCarts();

    CartDto getUserCart();

    CartDto updateProductQuantity(Long productId ,  int quantity);

    String deleteProductFromCart(Long cartId, Long productId);

    CartDto getCartByEmail(String userEmail);

    void updateProductInCarts(Long cartId, Long productId);

    CartDto deleteProductFromCurrentCart(Long productId);
}
