package com.ecomm.sb_ecomm.order.event;

import com.ecomm.sb_ecomm.order.services.OrderService;
import com.ecomm.sb_ecomm.payment.event.PaymentSucceededEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PaymentSucceededEventListener {
    private final OrderService orderService;

    public PaymentSucceededEventListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentSucceeded(PaymentSucceededEvent event){
        orderService.confirmOrderAfterPayment(event.orderId());
    }

}
