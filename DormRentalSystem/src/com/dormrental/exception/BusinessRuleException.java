package com.dormrental.exception;

/** Thrown when a request is well-formed but breaks a dorm rule (room full, tenant already leasing, ...). */
public class BusinessRuleException extends DormRentalException {
    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String message) {
        super(message);
    }
}
