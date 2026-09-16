package com.ecomm.sb_ecomm.payment.controller;

import com.ecomm.sb_ecomm.payment.services.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class PaymentController {

    private final PaymentService paymentService;
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payments/checkout/{orderId}")
    public ResponseEntity<?> checkout(@PathVariable Long orderId, @RequestParam String paymentMethod) {
        return new ResponseEntity<>(this.paymentService.initiatePayment(orderId,paymentMethod), HttpStatus.CREATED);
    }

}
