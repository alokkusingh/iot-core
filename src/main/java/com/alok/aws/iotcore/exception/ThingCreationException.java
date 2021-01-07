package com.alok.aws.iotcore.exception;

public class ThingCreationException extends RuntimeException {
    public ThingCreationException () {
        super();
    }

    public ThingCreationException(String messge) {
        super(messge);
    }

    public ThingCreationException(String message, Throwable cause) {
        super(message,cause);
    }
}
