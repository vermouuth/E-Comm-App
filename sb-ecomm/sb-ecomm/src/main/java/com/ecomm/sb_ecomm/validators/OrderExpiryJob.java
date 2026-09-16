package com.ecomm.sb_ecomm.validators;

import com.ecomm.sb_ecomm.order.model.OrderItem;
import com.ecomm.sb_ecomm.order.model.Orders;
import com.ecomm.sb_ecomm.product.model.Product;
import com.ecomm.sb_ecomm.order.model.OrderStatus;
import com.ecomm.sb_ecomm.order.repository.OrderRepository;
import com.ecomm.sb_ecomm.product.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OrderExpiryJob {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusTransitionValidator orderStatusTransitionValidator;

    public OrderExpiryJob(ProductRepository productRepository, OrderRepository orderRepository, OrderStatusTransitionValidator orderStatusTransitionValidator) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderStatusTransitionValidator = orderStatusTransitionValidator;
    }

    @Transactional
    @Scheduled(fixedRate = 60000)
    public void releaseExpiredReservations() {
        List<Orders> expiredOrders = orderRepository.getExpiredOrders(OrderStatus.PENDING,LocalDateTime.now());

        for(Orders order : expiredOrders){
            releaseStock(order.getOrderItems());
            orderStatusTransitionValidator.validateTransition(order.getOrderStatus(), OrderStatus.EXPIRED);
            order.setOrderStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);
        }
    }

    private void releaseStock(List<OrderItem> orderItems) {
        List<Long> productIds = orderItems
                .stream()
                .map(oi -> oi.getProduct().getId())
                .distinct()
                .toList();

        List<Product> products = productRepository.findAllByIdForUpdate(productIds);

        Map<Long,Integer> quantityByProduct =
                orderItems.stream()
                        .collect(
                                Collectors.groupingBy(
                                        oi -> oi.getProduct().getId(),
                                        Collectors.summingInt(
                                                OrderItem::getQuantity))

                        );


        for (Product p : products) {
            int reserved = quantityByProduct.getOrDefault(p.getId(), 0);
            p.setReservedQuantity(p.getReservedQuantity() - reserved);
        }
        productRepository.saveAll(products);
    }
}
