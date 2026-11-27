package com.example.casinoproject.payment;

import java.math.BigDecimal;

/**
 * Абстракція платіжного провайдера. Зараз є тільки FakePaymentGateway, але справжній провайдер
 * підключався б через цей самий інтерфейс без змін у PaymentService.
 */
public interface PaymentGateway {

    GatewayResponse charge(String cardNumber, int expMonth, int expYear, String cvv, BigDecimal amount);

    GatewayResponse payout(String cardNumber, BigDecimal amount);

    static String signaturePayload(GatewayResponse r) {
        return r.approved() + "|" + r.reference() + "|" + r.amount().toPlainString() + "|" + r.message();
    }
}
