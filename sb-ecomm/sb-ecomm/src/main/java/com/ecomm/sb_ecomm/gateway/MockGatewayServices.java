package com.ecomm.sb_ecomm.gateway;

import java.math.BigDecimal;

public interface MockGatewayServices {
    GatewayInitResult initiate(Long paymentId , BigDecimal amount, String currency , String apiKey);
}
