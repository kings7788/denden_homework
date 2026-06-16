package com.bryant.denden_homework.exception;

/** Raised when an email provider fails to deliver a message (mapped to HTTP 502). */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
