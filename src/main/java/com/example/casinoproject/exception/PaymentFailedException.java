package com.example.casinoproject.exception;

import org.springframework.http.HttpStatus;

/**
 * Кидається, коли платіжний шлюз відмовив або не відповів. Через нього відкочується транзакція виведення коштів.
 */
public class PaymentFailedException extends CasinoException {

    public PaymentFailedException(String message) {
        super(HttpStatus.PAYMENT_REQUIRED, message);
    }
}
