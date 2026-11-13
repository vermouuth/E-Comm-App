package com.ecomm.sb_ecomm.payment.services;

import com.ecomm.sb_ecomm.config.AuthUtils;
import com.ecomm.sb_ecomm.exceptions.newexceptions.ApiException;
import com.ecomm.sb_ecomm.exceptions.newexceptions.ResourceNotFoundException;
import com.ecomm.sb_ecomm.gateway.GatewayInitResult;
import com.ecomm.sb_ecomm.gateway.MockGatewayServices;
import com.ecomm.sb_ecomm.order.model.Orders;
import com.ecomm.sb_ecomm.order.model.OrderStatus;
import com.ecomm.sb_ecomm.payment.event.PaymentSucceededEvent;
import com.ecomm.sb_ecomm.payment.model.PaymentMethod;
import com.ecomm.sb_ecomm.payment.model.PaymentStatus;
import com.ecomm.sb_ecomm.payment.payload.PaymentDto;
import com.ecomm.sb_ecomm.order.repository.OrderRepository;
import com.ecomm.sb_ecomm.payment.model.Payment;
import com.ecomm.sb_ecomm.payment.repository.PaymentRepository;
import com.ecomm.sb_ecomm.product.repository.ProductRepository;
import com.ecomm.sb_ecomm.order.services.OrderService;
import com.ecomm.sb_ecomm.validators.PaymentStatusTransitionValidator;
import org.springframework.transaction.annotation.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class PaymentServicesImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final ModelMapper modelMapper;
    private final OrderRepository orderRepository;
    private final PaymentStatusTransitionValidator paymentStatusTransitionValidator;
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final MockGatewayServices mockGatewayServices;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthUtils authUtils;

    @Value("${mock.gateway.api-key}")
    private String gatewayApiKey;
    @Value("${mock.gateway.currency}")
    private String defaultCurrency;

    public PaymentServicesImpl(PaymentRepository paymentRepository, ModelMapper modelMapper,
                               OrderRepository orderRepository,
                               PaymentStatusTransitionValidator paymentStatusTransitionValidator, ProductRepository productRepository,
                               OrderService orderService, MockGatewayServices mockGatewayServices, ApplicationEventPublisher eventPublisher, AuthUtils authUtils) {
        this.modelMapper = modelMapper;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.paymentStatusTransitionValidator = paymentStatusTransitionValidator;
        this.productRepository = productRepository;
        this.orderService = orderService;
        this.mockGatewayServices = mockGatewayServices;
        this.eventPublisher = eventPublisher;
        this.authUtils = authUtils;
    }




    private Map<String , PaymentMethod> PAYMENT_METHODS = Map.of("CASH ON DELIVERY", PaymentMethod.CashOnDelivery,
                "DEBIT CARD" , PaymentMethod.DebitCard,
                "CREDIT CARD" , PaymentMethod.CreditCard );

    private PaymentDto getPaymentDto(Payment payment){
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setId(payment.getId());
        paymentDto.setPaymentMethod(payment.getPaymentMethod().toString());
        paymentDto.setPaymentStaus(payment.getPaymentStatus().toString());
        paymentDto.setAmount(payment.getAmount());
        paymentDto.setCurrency(payment.getCurrency());
        paymentDto.setPgPaymentId(payment.getPgPaymentId());
        return paymentDto;
    }


    @Override
    @Transactional
    public PaymentDto initiatePayment(Long orderId , String paymentMethod) {

        PaymentMethod method = this.PAYMENT_METHODS.get(paymentMethod.trim().toUpperCase());
        if (method == null) {
            throw new ApiException("Unsupported payment method: " + paymentMethod + ". Use one of " + PAYMENT_METHODS.keySet());
        }

        // Scope the lookup to the caller: someone else's order looks exactly like a missing one.
        Long userId = this.authUtils.loggedInUserId();
        Orders freshPlacedOrder = this.orderRepository
                .findByIdAndUserId(orderId, userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Order" , "orderId" , orderId)
                );

        if (freshPlacedOrder.getOrderStatus() != OrderStatus.PENDING) {
            throw new ApiException("Order is not payable in status: " + freshPlacedOrder.getOrderStatus());
        }

        // A retried checkout must not open a second payment (the customer could be charged twice).
        Payment existing = freshPlacedOrder.getPayment();
        if (existing != null && (existing.getPaymentStatus() == PaymentStatus.INITIATED
                || existing.getPaymentStatus() == PaymentStatus.PENDING)) {
            return this.getPaymentDto(existing);
        }

        BigDecimal amount = BigDecimal.valueOf(freshPlacedOrder.getTotalAmount());

        Payment currentPayment = new Payment();
        currentPayment.setPaymentMethod(method);
        currentPayment.setUser(freshPlacedOrder.getUser());
        currentPayment.setPaymentStatus(PaymentStatus.INITIATED);
        currentPayment.setAmount(amount);
        currentPayment.setCurrency(defaultCurrency);
        this.paymentRepository.saveAndFlush(currentPayment);


        if(method == PaymentMethod.CashOnDelivery) {
            currentPayment.setPaymentStatus(PaymentStatus.PENDING);
            freshPlacedOrder.setPayment(currentPayment);
            this.orderRepository.save(freshPlacedOrder);
            // Paid on delivery: confirm now so OrderExpiryJob doesn't expire the order and release its stock.
            this.orderService.confirmOrderAfterPayment(orderId);
            return this.getPaymentDto(currentPayment);
        }

        GatewayInitResult result = this.mockGatewayServices.initiate(currentPayment.getId(),currentPayment.getAmount(),currentPayment.getCurrency(),gatewayApiKey);

        currentPayment.setPgPaymentId(result.gatewayReference());
        currentPayment.setPaymentStatus(PaymentStatus.PENDING);

        PaymentDto paymentDto = this.getPaymentDto(currentPayment);

        freshPlacedOrder.setPayment(currentPayment);
        this.orderRepository.save(freshPlacedOrder);

        paymentDto.setCheckoutUrl(result.checkoutUrl());
        return paymentDto;
    }

    @Override
    @Transactional
    public PaymentDto getPayment(Long userId, Long orderId) {
        Orders order = this.orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(
                () -> new ResourceNotFoundException("Order" , "orderId" , orderId));

        Payment payment = order.getPayment();
        if (payment == null) {
            throw new ResourceNotFoundException("Payment" , "orderId" , orderId);
        }

        return getPaymentDto(payment);
    }

    @Override
    public List<PaymentDto> getAllPayments() {
        return List.of();
    }

    @Override
    public List<PaymentDto> getAllUserPayments(Long userId) {
        return List.of();
    }

    private Payment getExistedPayment(Long paymentId){
        return  paymentRepository
                .findById(paymentId)
                .orElseThrow(
                        ()-> new ResourceNotFoundException("Payment" , "PaymentId", paymentId)
                );
    }

    private void nextStatusExecutions(Payment payment, PaymentStatus paymentStatus){
        if(paymentStatus == PaymentStatus.CAPTURED ||
                paymentStatus == PaymentStatus.PAID){
                eventPublisher.publishEvent(
                    new PaymentSucceededEvent(payment.getId(),
                            payment.getOrder().getId())
            );
        }
    }


    /*
    Assume two identical webhook requests arrive concurrently.
    Both requests may read the same Payment version and race to update it.


    One request wins and successfully updates the Payment.
    The other request loses the optimistic-locking race because the version
    has already changed, causing an ObjectOptimisticLockingFailureException.

    We don't retry the losing request because the winning transaction has
    already completed the operation. Instead, we can treat the losing request
    as successful as well, since the desired state has already been achieved.

    */
    @Override
    @Transactional
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50)
    )
    public PaymentDto updatePaymentStatus(Long paymentId, PaymentStatus nextStatus) {

        Payment payment = this.getExistedPayment(paymentId);

        PaymentStatus current = payment.getPaymentStatus();
        if(current == nextStatus)
            return this.getPaymentDto(payment);

        paymentStatusTransitionValidator.validatePaymentStatus(current, nextStatus);
        payment.setPaymentStatus(nextStatus);
        this.paymentRepository.save(payment);
        nextStatusExecutions(payment,nextStatus);

        return getPaymentDto(payment);

    }

    @Override
    public void deletePayment(Long orderId, Long userId) {
        
    }
}

