package com.dormrental.exception;

public class AuthenticationException extends DormRentalException {
    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
