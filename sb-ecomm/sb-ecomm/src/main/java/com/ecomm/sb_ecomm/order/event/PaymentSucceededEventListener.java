package com.ecomm.sb_ecomm.order.event;

import com.ecomm.sb_ecomm.order.services.OrderService;
import com.ecomm.sb_ecomm.payment.event.PaymentSucceededEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PaymentSucceededEventListener {
    private final OrderService orderService;

    public PaymentSucceededEventListener(OrderService orderService) {
        this.orderService = orderService;
    }

    // BEFORE_COMMIT runs inside the payment's transaction: if confirming the order fails, the payment update
    // and the webhook claim roll back too, the webhook answers 500, and the gateway retries it.
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handlePaymentSucceeded(PaymentSucceededEvent event){
        orderService.confirmOrderAfterPayment(event.orderId());
    }

}
