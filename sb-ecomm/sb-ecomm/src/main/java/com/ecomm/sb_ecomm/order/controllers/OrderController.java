package com.ecomm.sb_ecomm.order.controllers;

import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;

import com.ecomm.sb_ecomm.config.OpenApiConfig;
import com.ecomm.sb_ecomm.order.payload.OrderDto;
import com.ecomm.sb_ecomm.order.services.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Orders", description = "Place orders from the current user's cart. Requires authentication.")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@Validated
@RestController
@RequestMapping("api/")
public class OrderController {

    private final OrderService orderService;
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Place an order from the current user's cart",
            description = "Requires an Idempotency-Key header to make retried requests safe to resend.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order placed",
                    content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @ApiResponse(responseCode = "400", description = "Cart is empty or invalid"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("user/orders/placeOrder")
    public ResponseEntity<?> placeOrder(
            @Parameter(in = ParameterIn.HEADER, name = "Idempotency-Key", description = "Client-generated key that lets retried requests be recognized as duplicates", required = true)
            @NotBlank(message = "Idempotency-Key header is required") @RequestHeader("Idempotency-Key") String idempotencyKey){
            return new ResponseEntity<>(orderService.placeOrder(idempotencyKey), HttpStatus.CREATED);
    }

    @Operation(summary = "List the current user's orders", description = "Newest first.")
    @ApiResponse(responseCode = "200", description = "Current user's orders",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrderDto.class))))
    @GetMapping("user/orders")
    public ResponseEntity<?> getUserOrders(){
        return ResponseEntity.ok(orderService.getCurrentUserOrders());
    }

    @Operation(summary = "Get one of the current user's orders")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found",
                    content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @ApiResponse(responseCode = "404", description = "Order not found (or not the current user's)",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @GetMapping("user/orders/{orderId}")
    public ResponseEntity<?> getUserOrder(@Parameter(description = "Order id") @Positive(message = "orderId must be a positive number") @PathVariable Long orderId){
        return ResponseEntity.ok(orderService.getCurrentUserOrder(orderId));
    }

}
