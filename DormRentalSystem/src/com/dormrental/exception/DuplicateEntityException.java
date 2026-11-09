package com.dormrental.exception;

public class DuplicateEntityException extends DormRentalException {
    private static final long serialVersionUID = 1L;

    public DuplicateEntityException(String message) {
        super(message);
    }
}
