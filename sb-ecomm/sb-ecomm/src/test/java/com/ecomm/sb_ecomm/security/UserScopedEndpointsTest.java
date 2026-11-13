package com.ecomm.sb_ecomm.security;

import com.ecomm.sb_ecomm.address.model.Address;
import com.ecomm.sb_ecomm.address.payload.AddressDto;
import com.ecomm.sb_ecomm.address.repository.AddressRepository;
import com.ecomm.sb_ecomm.address.services.AddressServicesImpl;
import com.ecomm.sb_ecomm.auth.model.Users;
import com.ecomm.sb_ecomm.auth.repository.UserRepository;
import com.ecomm.sb_ecomm.cart.model.Cart;
import com.ecomm.sb_ecomm.cart.model.CartItem;
import com.ecomm.sb_ecomm.cart.payload.CartDto;
import com.ecomm.sb_ecomm.cart.repository.CartItemRepository;
import com.ecomm.sb_ecomm.cart.repository.CartRepository;
import com.ecomm.sb_ecomm.cart.services.CartServicesImpl;
import com.ecomm.sb_ecomm.category.repository.CategoryRepository;
import com.ecomm.sb_ecomm.config.AuthUtils;
import com.ecomm.sb_ecomm.exceptions.newexceptions.ApiException;
import com.ecomm.sb_ecomm.exceptions.newexceptions.ResourceNotFoundException;
import com.ecomm.sb_ecomm.order.model.Orders;
import com.ecomm.sb_ecomm.order.payload.OrderDto;
import com.ecomm.sb_ecomm.order.repository.OrderRepository;
import com.ecomm.sb_ecomm.order.services.OrderServiceImpl;
import com.ecomm.sb_ecomm.product.model.Product;
import com.ecomm.sb_ecomm.product.payload.ProductDto;
import com.ecomm.sb_ecomm.product.repository.ProductRepository;
import com.ecomm.sb_ecomm.product.services.FileService;
import com.ecomm.sb_ecomm.product.services.ProductServiceImpl;
import com.ecomm.sb_ecomm.cart.services.CartServices;
import com.ecomm.sb_ecomm.validators.OrderStatusTransitionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The user-facing endpoints added for the storefront: every one of them must be scoped to the
 * caller, and someone else's resource must look exactly like a missing one.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserScopedEndpointsTest {

    private static final long ME = 1L;

    @Mock AuthUtils authUtils;
    @Mock ModelMapper modelMapper;
    @Mock AddressRepository addressRepository;
    @Mock UserRepository userRepository;
    @Mock OrderRepository orderRepository;
    @Mock ProductRepository productRepository;
    @Mock OrderStatusTransitionValidator orderStatusTransitionValidator;
    @Mock CartRepository cartRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock CartServices cartServices;
    @Mock FileService fileService;

    AddressServicesImpl addressServices;
    OrderServiceImpl orderService;
    CartServicesImpl cartServicesImpl;
    ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        addressServices = new AddressServicesImpl(addressRepository, userRepository, authUtils, modelMapper);
        orderService = new OrderServiceImpl(orderRepository, modelMapper, authUtils, productRepository,
                orderStatusTransitionValidator, cartRepository, addressRepository);
        cartServicesImpl = new CartServicesImpl(cartRepository, productRepository, cartItemRepository, modelMapper, authUtils);
        productService = new ProductServiceImpl(productRepository, categoryRepository, cartServices,
                modelMapper, fileService, cartRepository);

        when(authUtils.loggedInUserId()).thenReturn(ME);
        when(authUtils.loggedInEmail()).thenReturn("me@example.com");
        when(modelMapper.map(any(Address.class), eq(AddressDto.class))).thenReturn(new AddressDto());
        when(modelMapper.map(any(Cart.class), eq(CartDto.class))).thenReturn(new CartDto());
        when(modelMapper.map(any(Product.class), eq(ProductDto.class))).thenReturn(new ProductDto());
    }

    // ---- addresses ----

    private Address address(long id, boolean isDefault) {
        Address a = new Address("1 Old Street", "Cairo", "Cairo", "111111", "EG");
        a.setId(id);
        a.setIsDefault(isDefault);
        return a;
    }

    private AddressDto newDetails() {
        AddressDto dto = new AddressDto();
        dto.setStreet("99 New Street");
        dto.setCity("Giza");
        dto.setState("Giza");
        dto.setZip("222222");
        dto.setCountry("EG");
        dto.setIsDefault(false); // must be ignored
        return dto;
    }

    @Test
    void updateUserAddress_ofSomeoneElse_isNotFound_andSavesNothing() {
        when(addressRepository.findByUsersIdAndId(ME, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressServices.updateUserAddress(newDetails(), 5L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(addressRepository, never()).save(any());
    }

    @Test
    void updateUserAddress_ofMine_changesFields_butNotTheDefaultFlag() {
        Address mine = address(5L, true);
        when(addressRepository.findByUsersIdAndId(ME, 5L)).thenReturn(Optional.of(mine));
        when(addressRepository.save(mine)).thenReturn(mine);

        addressServices.updateUserAddress(newDetails(), 5L);

        assertThat(mine.getStreet()).isEqualTo("99 New Street");
        assertThat(mine.getCity()).isEqualTo("Giza");
        assertThat(mine.getIsDefault()).isTrue();
        verify(addressRepository).save(mine);
    }

    @Test
    void deleteUserAddress_ofSomeoneElse_isNotFound_andDeletesNothing() {
        when(addressRepository.findByUsersIdAndId(ME, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressServices.deleteUserAddress(5L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(addressRepository, never()).delete(any());
    }

    @Test
    void deleteUserAddress_ofMine_deletesIt() {
        Address mine = address(5L, false);
        when(addressRepository.findByUsersIdAndId(ME, 5L)).thenReturn(Optional.of(mine));

        addressServices.deleteUserAddress(5L);

        verify(addressRepository).delete(mine);
    }

    // ---- orders ----

    private Orders order(long id) {
        Orders o = new Orders();
        o.setId(id);
        o.setEmail("me@example.com");
        o.setTotalAmount(100.0);
        o.setOrderItems(new ArrayList<>());
        return o;
    }

    @Test
    void getCurrentUserOrder_ofSomeoneElse_isNotFound() {
        when(orderRepository.findByIdAndUserId(10L, ME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getCurrentUserOrder(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCurrentUserOrder_ofMine_returnsIt() {
        when(orderRepository.findByIdAndUserId(10L, ME)).thenReturn(Optional.of(order(10L)));

        assertThat(orderService.getCurrentUserOrder(10L).getOrderId()).isEqualTo(10L);
    }

    @Test
    void getCurrentUserOrders_isNewestFirst_andOnlyMine() {
        when(orderRepository.getOrdersByUserId(ME)).thenReturn(List.of(order(3L), order(9L), order(5L)));

        List<OrderDto> result = orderService.getCurrentUserOrders();

        assertThat(result).extracting(OrderDto::getOrderId).containsExactly(9L, 5L, 3L);
        verify(orderRepository).getOrdersByUserId(ME);
    }

    // ---- cart quantity ----

    private Cart cartWith(long productId, int stock, int quantity) {
        Product product = new Product();
        product.setId(productId);
        product.setProductName("Widget");
        product.setStockQuantity(stock);

        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setProductPrice(10.0);

        Cart cart = new Cart();
        cart.setCartItems(new ArrayList<>(List.of(item)));
        when(cartRepository.findByUserEmail("me@example.com")).thenReturn(cart);
        return cart;
    }

    @Test
    void updateProductQuantity_aboveStock_isRejected_andKeepsTheOldQuantity() {
        Cart cart = cartWith(7L, 5, 2);

        assertThatThrownBy(() -> cartServicesImpl.updateProductQuantity(7L, 6))
                .isInstanceOf(ApiException.class);

        assertThat(cart.getCartItems().getFirst().getQuantity()).isEqualTo(2);
    }

    @Test
    void updateProductQuantity_withinStock_overwritesTheQuantity_andTotal() {
        Cart cart = cartWith(7L, 5, 2);

        cartServicesImpl.updateProductQuantity(7L, 5);

        assertThat(cart.getCartItems().getFirst().getQuantity()).isEqualTo(5);
        assertThat(cart.getTotalPrice()).isEqualTo(50.0);
    }

    @Test
    void updateProductQuantity_zero_removesTheLine() {
        Cart cart = cartWith(7L, 5, 2);

        cartServicesImpl.updateProductQuantity(7L, 0);

        assertThat(cart.getCartItems()).isEmpty();
        assertThat(cart.getTotalPrice()).isZero();
    }

    @Test
    void updateProductQuantity_forAProductNotInMyCart_isNotFound() {
        cartWith(7L, 5, 2);

        assertThatThrownBy(() -> cartServicesImpl.updateProductQuantity(99L, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- product detail ----

    @Test
    void getProductById_unknown_isNotFound() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getProductById_known_returnsIt() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(new Product()));

        assertThat(productService.getProductById(1L)).isNotNull();
    }
}
