package com.ecomm.sb_ecomm.payment.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDto {

    private Long paymentId;
    private String pgPaymentId;
    private String paymentMethod;
    private String paymentStaus;
    private String checkoutUrl;
    private BigDecimal amount;
    private String currency;
}
