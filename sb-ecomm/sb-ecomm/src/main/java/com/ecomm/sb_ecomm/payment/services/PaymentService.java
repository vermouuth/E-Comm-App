package com.ecomm.sb_ecomm.payment.services;

import com.ecomm.sb_ecomm.payment.model.PaymentStatus;
import com.ecomm.sb_ecomm.payment.payload.PaymentDto;
import java.util.List;


public interface PaymentService {

    PaymentDto getPayment(Long userId, Long orderId);
    List<PaymentDto> getAllPayments();
    List<PaymentDto> getAllUserPayments(Long userId);
    PaymentDto updatePaymentStatus(Long paymentId, PaymentStatus paymentStatus);
    PaymentDto initiatePayment(Long orderId , String enPaymentMethod);
    void deletePayment(Long orderId, Long userId);
}

