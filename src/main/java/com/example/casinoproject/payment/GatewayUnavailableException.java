package com.example.casinoproject.payment;

/**
 * Шлюз не відповів (у фейковому шлюзі імітується карткою 4000 0000 0000 0119).
 */
public class GatewayUnavailableException extends RuntimeException {

    public GatewayUnavailableException(String message) {
        super(message);
    }
}
