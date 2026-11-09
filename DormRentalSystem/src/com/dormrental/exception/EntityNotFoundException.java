package com.dormrental.exception;

public class EntityNotFoundException extends DormRentalException {
    private static final long serialVersionUID = 1L;

    public EntityNotFoundException(String entityName, String id) {
        super(entityName + " not found: " + id);
    }
}
