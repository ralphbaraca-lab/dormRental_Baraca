package com.dormrental.exception;

/** Base class for every error this application raises on purpose. */
public class DormRentalException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DormRentalException(String message) {
        super(message);
    }

    public DormRentalException(String message, Throwable cause) {
        super(message, cause);
    }
}
