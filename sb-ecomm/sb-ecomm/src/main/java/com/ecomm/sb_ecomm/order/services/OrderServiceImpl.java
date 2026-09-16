package com.ecomm.sb_ecomm.order.services;

import com.ecomm.sb_ecomm.address.model.Address;
import com.ecomm.sb_ecomm.address.repository.AddressRepository;
import com.ecomm.sb_ecomm.auth.model.Users;
import com.ecomm.sb_ecomm.cart.model.Cart;
import com.ecomm.sb_ecomm.cart.repository.CartRepository;
import com.ecomm.sb_ecomm.config.AuthUtils;
import com.ecomm.sb_ecomm.exceptions.newexceptions.ApiException;
import com.ecomm.sb_ecomm.order.model.OrderItem;
import com.ecomm.sb_ecomm.order.model.Orders;
import com.ecomm.sb_ecomm.order.model.OrderStatus;
import com.ecomm.sb_ecomm.order.payload.OrderDto;
import com.ecomm.sb_ecomm.product.model.Product;
import com.ecomm.sb_ecomm.product.payload.ProductDto;
import com.ecomm.sb_ecomm.order.repository.OrderRepository;
import com.ecomm.sb_ecomm.product.repository.ProductRepository;
import com.ecomm.sb_ecomm.validators.OrderStatusTransitionValidator;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl  implements OrderService {


    private OrderRepository orderRepository;
    private ModelMapper modelMapper;
    private AuthUtils authUtils;
    private ProductRepository productRepository;
    private final OrderStatusTransitionValidator orderStatusTransitionValidator;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;

    public OrderServiceImpl(OrderRepository orderRepository, ModelMapper modelMapper
            , AuthUtils authUtils , ProductRepository productRepository, OrderStatusTransitionValidator orderStatusTransitionValidator, CartRepository cartRepository, AddressRepository addressRepository) {
        this.orderRepository = orderRepository;
        this.modelMapper = modelMapper;
        this.authUtils = authUtils;
        this.productRepository = productRepository;
        this.orderStatusTransitionValidator = orderStatusTransitionValidator;
        this.cartRepository = cartRepository;
        this.addressRepository = addressRepository;
    }

    private void confirmReservedProducts(Orders order)
    {

        List<Long> productIds = order.getOrderItems().stream()
                .map(item -> item.getProduct().getId())
                .toList();

        List<Product> productList = productRepository.findAllByIdForUpdate(productIds);

        Map<Long, Product> productMap = productList.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        Function.identity()
                ));

        order.getOrderItems().forEach(item -> {
            Product product = productMap.get(item.getProduct().getId());
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            product.setReservedQuantity(product.getReservedQuantity() - item.getQuantity());
        });

        this.productRepository.saveAll(productList);
    }

    private void reserveItemQuantity(List<OrderItem> orderItems) {

        List<Long> productIdes = orderItems.stream().map(
                orderItem -> orderItem.getProduct().getId()
        ).toList();

        List<Product> products = this.productRepository.findAllByIdForUpdate(productIdes);

        Map<Long,Product> productMap = new HashMap<>();
        for (Product product : products) {
            productMap.put(product.getId(), product);
        }

        for(OrderItem oi : orderItems) {
            Product product = productMap.get(oi.getProduct().getId());
            int freeStock = product.getStockQuantity() - product.getReservedQuantity();
            if(freeStock < oi.getQuantity()) {
                throw new ApiException("this item with id " + product.getId() + " is not available with this quantity , " +
                        "the available quantity is  " + freeStock);
            }
        }

        for(OrderItem oi : orderItems) {
            Product product = productMap.get(oi.getProduct().getId());
            Integer newReserved  = product.getReservedQuantity() + oi.getQuantity();
            product.setReservedQuantity(newReserved);
        }

        this.productRepository.saveAll(products);

    }

    private double recalculateOrderAmount(List<OrderItem> orderItems) {
        return orderItems.stream().mapToDouble(oi -> oi.getProductPrice() * oi.getQuantity()).sum();
    }
    private Orders getExistedOrder(Long orderId)
    {
        return this.orderRepository.findById(orderId).orElseThrow(
                () -> new ApiException("Order with id " + orderId + " not found!"));
    }

    private Orders getExistedOrder(Long orderId, Long userId)
    {
        return this.orderRepository.findByIdAndUserId(orderId,userId).orElseThrow(
                () -> new ApiException("Order with id " + orderId + " not found!"));
    }

    private Optional<Orders> getExistedOrder(String idempotencyKey, Long userId)
    {
        return this.orderRepository
                .findByIdempotencyKeyAndUserIdFetchItems(idempotencyKey,userId);
    }

    private Cart getExistedCart(String email)
    {
        Cart cart = cartRepository.findByUserEmail(email);
        if (cart == null) throw new ApiException("Cart doesn't exist");
        if (cart.getCartItems().isEmpty()) throw new ApiException("Cart is empty, add items to place an order.");
        return cart;
    }

    private Address getExistedAddress(Long userId)
    {
        return addressRepository.findByUsersIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new ApiException("Default address not found"));
    }

    private List<OrderItem> getOrderItemsFromCart(Cart cart)
    {
        return cart.getCartItems().stream()
                .map(ci -> {
                    OrderItem oi = new OrderItem();
                    oi.setProduct(ci.getProduct());
                    oi.setQuantity(ci.getQuantity());
                    oi.setProductPrice(ci.getProductPrice());
                    oi.setDiscount(ci.getDiscount());
                    return oi;
                }).toList();
    }

    private OrderDto getOrderDto(Orders order) {
        OrderDto dto = new OrderDto();
        dto.setOrderId(order.getId());
        dto.setEmail(order.getEmail());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setOrderStatus(String.valueOf(order.getOrderStatus()));
        // address snapshot
        dto.setStreet(order.getStreet());
        dto.setCity(order.getCity());
        dto.setState(order.getState());
        dto.setZip(order.getZip());
        dto.setCountry(order.getCountry());

        // product list — you already build this by hand
        List<ProductDto> products = order.getOrderItems().stream()
                .map(oi -> {
                    ProductDto p = new ProductDto();
                    p.setProductName(oi.getProduct().getProductName());
                    p.setQuantity(oi.getQuantity());
                    p.setPrice(oi.getProductPrice());
                    p.setDiscount(oi.getDiscount());
                    return p;
                }).toList();
        dto.setProductDtoList(products);

        return dto;

    }

    @Transactional
    public OrderDto createOrder(String idempotencyKey, Users loggedInUser) {

        Cart cart = getExistedCart(loggedInUser.getEmail());

        Address defaultAddress = getExistedAddress(loggedInUser.getId());

        List<OrderItem> orderItems = getOrderItemsFromCart(cart);

        Orders order = new Orders();
        order.setCity(defaultAddress.getCity());
        order.setCountry(defaultAddress.getCountry());
        order.setState(defaultAddress.getState());
        order.setStreet(defaultAddress.getStreet());
        order.setZip(defaultAddress.getZip());
        order.setEmail(cart.getUniqueUser().getEmail());
        order.setOrderItems(orderItems);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setUser(cart.getUniqueUser());
        order.setIdempotencyKey(idempotencyKey);
        order.setTotalAmount(recalculateOrderAmount(orderItems));
        orderItems.forEach(oi -> oi.setOrders(order));
        order.setExpirationDate(LocalDateTime.now().plusMinutes(15));

        // 1. CLAIM THE KEY FIRST — flush the order so the unique constraint fires NOW,
        //    before any product row is locked. The loser fails here, cleanly, holding no locks.
        orderRepository.saveAndFlush(order);

        // 2. Only the winner reaches here. Now it's safe to lock products and reserve.
        reserveItemQuantity(orderItems);

        return getOrderDto(order);
    }

    @Override
    public List<OrderDto> getOrders() {

        return this.orderRepository.findAll().stream()
                .map(orders -> {
                   List<ProductDto> productDtoList = orders.getOrderItems().stream()
                           .map(orderItem -> {
                               ProductDto productDto = modelMapper.map(orderItem.getProduct(), ProductDto.class);
                               productDto.setQuantity(orderItem.getQuantity());
                               return productDto;
                           }).toList();
                   OrderDto orderDto = this.modelMapper.map(orders, OrderDto.class);
                   orderDto.setProductDtoList(productDtoList);
                   return orderDto;
                }).toList();
    }

    @Override
    public List<OrderDto> getUserOrders(Long userId) {
        return this.orderRepository.getOrdersByUserId(userId)
                .stream().map(this::getOrderDto)
                .toList();
    }

    @Override
    @Transactional
    public OrderDto findById(Long id) {
        return getOrderDto(getExistedOrder(id));
    }

    @Override
    @Transactional
    public OrderDto findOrderByUserIdAndOrderId(Long orderId, Long userId) {
        return getOrderDto(getExistedOrder(orderId, userId));
    }

    @Override
    public OrderDto placeOrder(String idempotencyKey) {

        Users loggedInUser = this.authUtils.loggedInUser();

       // FAST PATH
       Optional<Orders> existedOrder = getExistedOrder(idempotencyKey, loggedInUser.getId());
       if(existedOrder.isPresent()) {
           return this.getOrderDto(existedOrder.get());
       }

       try {
         return createOrder(idempotencyKey, loggedInUser);
       }
       catch (DataIntegrityViolationException e) {
           return orderRepository.findByIdempotencyKeyAndUserIdFetchItems(idempotencyKey, loggedInUser.getId())
                   .map(this::getOrderDto)
                   .orElseThrow(() -> e);
       }

    }

    @Transactional
    @Override
    public OrderDto updateOrderStatus(OrderStatus orderStatus, Long orderId) {

        Orders order = this.orderRepository.findById(orderId).orElseThrow(
        () -> new ApiException("Order with id " + orderId + " not found!")
        );

        if(!this.orderStatusTransitionValidator.validateTransition(order.getOrderStatus(), orderStatus))
            throw new ApiException("Invalid order status transition!.");

        order.setOrderStatus(orderStatus);
        this.orderRepository.save(order);
        return getOrderDto(order);
    }

    @Override
    public void confirmOrderAfterPayment(Long orderId) {
        Orders order = getExistedOrder(orderId);

        confirmReservedProducts(order);

        updateOrderStatus(
                OrderStatus.CONFIRMED,
                orderId
        );
    }


    @Override
    public Void deleteOrder(Long id, Long userId) {
        return null;
    }
}
