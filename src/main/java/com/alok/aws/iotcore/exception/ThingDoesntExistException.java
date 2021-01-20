package com.alok.aws.iotcore.exception;

public class ThingDoesntExistException extends RuntimeException {
    public ThingDoesntExistException() {
        super();
    }

    public ThingDoesntExistException(String messge) {
        super(messge);
    }

    public ThingDoesntExistException(String message, Throwable cause) {
        super(message,cause);
    }
}
