package com.ecomm.sb_ecomm.payment.controller;

import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;

import com.ecomm.sb_ecomm.config.AuthUtils;
import com.ecomm.sb_ecomm.config.OpenApiConfig;
import com.ecomm.sb_ecomm.payment.payload.PaymentDto;
import com.ecomm.sb_ecomm.payment.services.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payments", description = "Checkout and initiate payment for an order. Requires authentication.")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@Validated
@RestController
@RequestMapping("/api/user")
public class PaymentController {

    private final PaymentService paymentService;
    private final AuthUtils authUtils;
    public PaymentController(PaymentService paymentService, AuthUtils authUtils) {
        this.paymentService = paymentService;
        this.authUtils = authUtils;
    }

    @Operation(summary = "Initiate payment for an order")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment initiated",
                    content = @Content(schema = @Schema(implementation = PaymentDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payment method or order state"),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @PostMapping("/payments/checkout/{orderId}")
    public ResponseEntity<?> checkout(@Parameter(description = "Order id") @Positive(message = "orderId must be a positive number") @PathVariable Long orderId,
                                       @Parameter(description = "Payment method identifier, e.g. \"card\"") @NotBlank(message = "paymentMethod is required") @RequestParam String paymentMethod) {
        return new ResponseEntity<>(this.paymentService.initiatePayment(orderId,paymentMethod), HttpStatus.CREATED);
    }

    @Operation(summary = "Get the payment of one of the current user's orders",
            description = "Poll this to follow the payment status (paymentStaus) after checkout. checkoutUrl is only returned by the checkout call.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found",
                    content = @Content(schema = @Schema(implementation = PaymentDto.class))),
            @ApiResponse(responseCode = "404", description = "Order not found, not the current user's, or no payment started yet",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @GetMapping("/payments/order/{orderId}")
    public ResponseEntity<?> getOrderPayment(@Parameter(description = "Order id") @Positive(message = "orderId must be a positive number") @PathVariable Long orderId) {
        return ResponseEntity.ok(this.paymentService.getPayment(this.authUtils.loggedInUserId(), orderId));
    }

}
