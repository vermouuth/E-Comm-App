package com.ecomm.sb_ecomm.order.services;

import com.ecomm.sb_ecomm.order.model.OrderStatus;
import com.ecomm.sb_ecomm.order.payload.OrderDto;

import java.util.List;


public interface OrderService {

    List<OrderDto> getOrders();
    List<OrderDto> getUserOrders(Long userId);
    OrderDto findById(Long id);
    OrderDto findOrderByUserIdAndOrderId(Long orderId , Long userId);
    /** The logged-in user's orders, newest first. */
    List<OrderDto> getCurrentUserOrders();
    /** One of the logged-in user's orders; someone else's order looks exactly like a missing one. */
    OrderDto getCurrentUserOrder(Long orderId);
    OrderDto placeOrder(String idempotencyKey);
    Void deleteOrder(Long id, Long userId);
    OrderDto updateOrderStatus(OrderStatus orderStatus , Long orderId);

    void confirmOrderAfterPayment(Long aLong);
}
