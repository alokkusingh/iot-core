package com.alok.aws.iotcore.exception;

public class CertificateDoesntExistException extends RuntimeException {
    public CertificateDoesntExistException() {
        super();
    }

    public CertificateDoesntExistException(String messge) {
        super(messge);
    }

    public CertificateDoesntExistException(String message, Throwable cause) {
        super(message,cause);
    }
}
