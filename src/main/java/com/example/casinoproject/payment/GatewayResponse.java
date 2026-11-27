package com.example.casinoproject.payment;

import java.math.BigDecimal;

/**
 * Відповідь платіжного шлюзу. signature це HMAC від решти полів, його треба перевірити
 * через PaymentGateway.signaturePayload перед тим, як довіряти approved.
 */
public record GatewayResponse(boolean approved, String reference, BigDecimal amount, String message, String signature) {
}
