package com.ecomm.sb_ecomm.security;

import com.ecomm.sb_ecomm.auth.model.Users;
import com.ecomm.sb_ecomm.config.AuthUtils;
import com.ecomm.sb_ecomm.exceptions.newexceptions.ResourceNotFoundException;
import com.ecomm.sb_ecomm.gateway.MockGatewayServices;
import com.ecomm.sb_ecomm.order.model.OrderStatus;
import com.ecomm.sb_ecomm.order.model.Orders;
import com.ecomm.sb_ecomm.order.repository.OrderRepository;
import com.ecomm.sb_ecomm.order.services.OrderService;
import com.ecomm.sb_ecomm.payment.model.Payment;
import com.ecomm.sb_ecomm.payment.model.PaymentMethod;
import com.ecomm.sb_ecomm.payment.model.PaymentStatus;
import com.ecomm.sb_ecomm.payment.payload.PaymentDto;
import com.ecomm.sb_ecomm.payment.repository.PaymentRepository;
import com.ecomm.sb_ecomm.payment.services.PaymentServicesImpl;
import com.ecomm.sb_ecomm.product.repository.ProductRepository;
import com.ecomm.sb_ecomm.validators.PaymentStatusTransitionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentOwnershipTest {

    private static final long ME = 1L;
    private static final long SOMEONE_ELSE = 2L;
    private static final long ORDER_ID = 10L;

    @Mock PaymentRepository paymentRepository;
    @Mock OrderRepository orderRepository;
    @Mock ProductRepository productRepository;
    @Mock ModelMapper modelMapper;
    @Mock PaymentStatusTransitionValidator validator;
    @Mock OrderService orderService;
    @Mock MockGatewayServices gateway;
    @Mock ApplicationEventPublisher publisher;
    @Mock AuthUtils authUtils;

    PaymentServicesImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentServicesImpl(paymentRepository, modelMapper, orderRepository, validator,
                productRepository, orderService, gateway, publisher, authUtils);
        ReflectionTestUtils.setField(service, "gatewayApiKey", "key");
        ReflectionTestUtils.setField(service, "defaultCurrency", "EGP");
        when(authUtils.loggedInUserId()).thenReturn(ME);
    }

    private Orders orderOwnedBy(long userId) {
        Users owner = new Users();
        owner.setId(userId);
        Orders order = new Orders();
        order.setId(ORDER_ID);
        order.setUser(owner);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setTotalAmount(500.0);
        return order;
    }

    /** Stubs the repository the way the database would behave for the given owner. */
    private void orderExists(Orders order) {
        long ownerId = order.getUser().getId();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.findByIdAndUserId(ORDER_ID, ownerId)).thenReturn(Optional.of(order));
    }

    @Test
    void initiatePayment_forSomeoneElsesOrder_isRejected_andCreatesNothing() {
        orderExists(orderOwnedBy(SOMEONE_ELSE));

        assertThatThrownBy(() -> service.initiatePayment(ORDER_ID, "CASH ON DELIVERY"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(paymentRepository, never()).saveAndFlush(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void initiatePayment_forMyOwnOrder_works() {
        orderExists(orderOwnedBy(ME));

        PaymentDto dto = service.initiatePayment(ORDER_ID, "CASH ON DELIVERY");

        assertThat(dto.getPaymentStaus()).isEqualTo(PaymentStatus.PENDING.toString());
        verify(paymentRepository).saveAndFlush(any(Payment.class));
    }

    @Test
    void getPayment_returnsThePaymentOfMyOrder() {
        Payment payment = new Payment();
        payment.setId(77L);
        payment.setPaymentMethod(PaymentMethod.CashOnDelivery);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setAmount(BigDecimal.valueOf(500));
        payment.setCurrency("EGP");
        Orders order = orderOwnedBy(ME);
        order.setPayment(payment);
        orderExists(order);

        PaymentDto dto = service.getPayment(ME, ORDER_ID);

        assertThat(dto.getId()).isEqualTo(77L);
    }

    @Test
    void getPayment_forSomeoneElsesOrder_isNotFound() {
        Payment payment = new Payment();
        payment.setId(77L);
        payment.setPaymentMethod(PaymentMethod.CashOnDelivery);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        Orders order = orderOwnedBy(SOMEONE_ELSE);
        order.setPayment(payment);
        orderExists(order);

        assertThatThrownBy(() -> service.getPayment(ME, ORDER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPayment_whenOrderHasNoPaymentYet_isNotFound() {
        orderExists(orderOwnedBy(ME));

        assertThatThrownBy(() -> service.getPayment(ME, ORDER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
