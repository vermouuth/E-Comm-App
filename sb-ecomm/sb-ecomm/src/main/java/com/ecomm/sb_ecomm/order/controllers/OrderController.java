package com.ecomm.sb_ecomm.order.controllers;

import com.ecomm.sb_ecomm.order.services.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/")
public class OrderController {

    private final OrderService orderService;
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("user/orders/placeOrder")
    public ResponseEntity<?> placeOrder(@RequestHeader("Idempotency-Key") String idempotencyKey){
            return new ResponseEntity<>(orderService.placeOrder(idempotencyKey), HttpStatus.CREATED);
    }

}
