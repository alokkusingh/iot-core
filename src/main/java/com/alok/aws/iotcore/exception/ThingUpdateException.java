package com.alok.aws.iotcore.exception;

public class ThingUpdateException extends RuntimeException {
    public ThingUpdateException() {
        super();
    }

    public ThingUpdateException(String messge) {
        super(messge);
    }

    public ThingUpdateException(String message, Throwable cause) {
        super(message,cause);
    }
}
