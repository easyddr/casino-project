package com.example.casinoproject.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Єдиний виняток бізнес-логіки. Статус одразу каже, яким HTTP-кодом відповісти,
 * тому в сервісах просто кидайте CasinoException.notFound("...") або подібне, а REST-обробник зробить решту.
 */
@Getter
public class CasinoException extends RuntimeException {

    private final HttpStatus status;

    public CasinoException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static CasinoException notFound(String message) {
        return new CasinoException(HttpStatus.NOT_FOUND, message);
    }

    public static CasinoException badRequest(String message) {
        return new CasinoException(HttpStatus.BAD_REQUEST, message);
    }

    public static CasinoException conflict(String message) {
        return new CasinoException(HttpStatus.CONFLICT, message);
    }

    public static CasinoException unprocessable(String message) {
        return new CasinoException(HttpStatus.valueOf(422), message);
    }

    public static CasinoException forbidden(String message) {
        return new CasinoException(HttpStatus.FORBIDDEN, message);
    }
}
