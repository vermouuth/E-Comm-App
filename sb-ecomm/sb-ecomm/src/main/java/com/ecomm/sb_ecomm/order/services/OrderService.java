package com.ecomm.sb_ecomm.order.services;

import com.ecomm.sb_ecomm.order.model.OrderStatus;
import com.ecomm.sb_ecomm.order.payload.OrderDto;

import java.util.List;


public interface OrderService {

    List<OrderDto> getOrders();
    List<OrderDto> getUserOrders(Long userId);
    OrderDto findById(Long id);
    OrderDto findOrderByUserIdAndOrderId(Long userId , Long orderId);
    OrderDto placeOrder(String idempotencyKey);
    Void deleteOrder(Long id, Long userId);
    OrderDto updateOrderStatus(OrderStatus orderStatus , Long orderId);

    void confirmOrderAfterPayment(Long aLong);
}
