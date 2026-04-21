package com.ecomm.sb_ecomm.services.impls;

import com.ecomm.sb_ecomm.config.AuthUtils;
import com.ecomm.sb_ecomm.exceptions.newexceptions.ApiException;
import com.ecomm.sb_ecomm.exceptions.newexceptions.ResourceNotFoundException;
import com.ecomm.sb_ecomm.models.*;
import com.ecomm.sb_ecomm.payload.dto.CartDto;
import com.ecomm.sb_ecomm.payload.dto.ProductDto;
import com.ecomm.sb_ecomm.repositories.CartItemRepository;
import com.ecomm.sb_ecomm.repositories.CartRepository;
import com.ecomm.sb_ecomm.repositories.ProductRepository;
import com.ecomm.sb_ecomm.services.CartServices;
import org.springframework.transaction.annotation.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class CartServicesImpl implements CartServices {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final ModelMapper modelMapper;
    private final AuthUtils authUtils;

    public CartServicesImpl(CartRepository cartRepository ,
                            ProductRepository productRepository ,
                            CartItemRepository cartItemRepository ,
                            ModelMapper modelMapper ,
                            AuthUtils authUtils) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.modelMapper = modelMapper;
        this.authUtils = authUtils;
    }

    private Cart getOrCreateCart() {

       Cart cart = this.cartRepository.findByUserEmail(this.authUtils.loggedInEmail());
       if (cart != null)
           return cart;

        Cart newCart = new Cart();
        newCart.setUser(authUtils.loggedInUser());
        newCart.setTotalPrice(0.00);
        return cartRepository.save(newCart);

    }

    @Override
    public CartDto addProductToCart(Long productId, int quantity) {

        Cart cart = getOrCreateCart();

       Product productFromDB = productRepository.findById(productId).
               orElseThrow(() -> new ResourceNotFoundException("Product" , "ProductId", productId));

       if(productFromDB.getQuantity() == 0 )
           throw new ApiException("Product " + productFromDB.getProductName() + " is not available!");

       CartItem  cartItem = cartItemRepository.findByCartIdAndProductId(cart.getCartId(), productId);

       if(cartItem != null)
           throw  new ApiException("Product " + productFromDB.getProductName() + " is already in Cart");

       if(quantity > productFromDB.getQuantity())
           throw  new ApiException("Please , make an order of the "  + productFromDB.getProductName() +
                   " less than or equal to the quantity "  + productFromDB.getQuantity() + ".");


      CartItem newCartItem = new CartItem();

      newCartItem.setProduct(productFromDB);
      newCartItem.setQuantity(quantity);
      newCartItem.setCart(cart);
      newCartItem.setDiscount(productFromDB.getDiscount());
      newCartItem.setProductPrice(productFromDB.getSpecialPrice());
      cartItemRepository.save(newCartItem);

      cart.getCartItems().add(newCartItem);
      cart.setTotalPrice(cart.getTotalPrice() + (productFromDB.getSpecialPrice() * quantity));
      cartRepository.save(cart);

      CartDto cartDto = modelMapper.map(cart, CartDto.class);

     List<CartItem> cartItems = cart.getCartItems();

     Stream<ProductDto> productDtoStream = cartItems.stream().map(
            item ->
            {
                ProductDto map = modelMapper.map(item.getProduct(), ProductDto.class);
                map.setQuantity(item.getQuantity());
                return map;
            });

     cartDto.setProducts(productDtoStream.toList());

     return cartDto;


    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public List<CartDto> getCarts() {

        List<Cart> carts = cartRepository.findAll();
        if(carts.isEmpty())
            throw new ApiException("there is no carts it has been added");


        List<CartDto> cartDtoList = carts.stream().map(cart -> {
            CartDto cartDto = modelMapper.map(cart, CartDto.class);

            List<ProductDto> productDtoList = cart.getCartItems().stream()
                    .map(cartItem -> modelMapper.map(cartItem.getProduct(), ProductDto.class))
                    .toList();

            cartDto.setProducts(productDtoList);

            return cartDto;
        }).toList();

        return cartDtoList;
    }

    @Override
    @Transactional
    public CartDto getUserCart() {
        User user = authUtils.loggedInUser();
        if (user == null)
            throw new ApiException("User not logged in");


        Cart userCart = this.getOrCreateCart();
        CartDto cartDto = this.modelMapper.map(userCart,CartDto.class);

        List<ProductDto> productsDtoList = userCart.getCartItems().stream()
                        .map(cartItem -> modelMapper.map(cartItem.getProduct(),ProductDto.class))
                                .toList();

        cartDto.setProducts(productsDtoList);
        return cartDto;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public CartDto getCartByEmail(String userEmail) {


        Cart cart = cartRepository.findByUserEmail(userEmail);

        if (cart == null)
            throw new ResourceNotFoundException("Cart" ,  "UserEmail", userEmail);

        CartDto cartDto = this.modelMapper.map(cart,CartDto.class);

        List<ProductDto> productDtoList = cart.getCartItems().stream()
                .map(item -> this.modelMapper.map(item.getProduct(),ProductDto.class)).toList();

        cartDto.setProducts(productDtoList);

        return cartDto;

    }

    @Transactional
    @Override
    public CartDto updateProductQuantity(Long productId, int quantity) {

        if (quantity < 0)
            throw new ApiException("Quantity must not be negative");

        Cart userCart = getOrCreateCart();

        CartItem cartItem = userCart.getCartItems().stream()
                .filter(item -> item.getProduct().getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "productId", productId));

        if (quantity == 0) {
            userCart.getCartItems().remove(cartItem); // orphanRemoval handles delete
        } else {
            cartItem.setQuantity(quantity);
        }

        // correct total calculation
        double total = userCart.getCartItems().stream()
                .mapToDouble(item -> item.getProductPrice() * item.getQuantity())
                .sum();

        userCart.setTotalPrice(total);

        // mapping
        CartDto cartDto = modelMapper.map(userCart, CartDto.class);

        Stream<ProductDto> productDtoList = userCart.getCartItems().stream()
                .map(item -> {
                  ProductDto productDto =   modelMapper.map(item.getProduct(), ProductDto.class);
                  productDto.setQuantity(item.getQuantity());
                  return productDto;
                });

        cartDto.setProducts(productDtoList.toList());

        return cartDto;
    }


    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        if (cartItemRepository.findByCartIdAndProductId(cartId, productId) == null)
            throw new ResourceNotFoundException("CartItem", "productId", productId);

        cartItemRepository.deleteByCartIdAndProductId(cartId, productId);

        double total = cart.getCartItems().stream()
                .filter(item -> !item.getProduct().getProductId().equals(productId))
                .mapToDouble(item -> item.getProductPrice() * item.getQuantity())
                .sum();

        cart.setTotalPrice(total);
        cartRepository.save(cart);

        return "Product " + productId + " removed from cart " + cartId + " for user: " + cart.getUser().getEmail();
    }


}
