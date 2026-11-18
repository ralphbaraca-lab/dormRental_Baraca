
package com.dormrental.exception;

/**
 * Thrown when user-provided data fails validation.
 *
 * Examples include:
 * - Missing required fields.
 * - Invalid input formats.
 * - Values outside acceptable ranges.
 * - Invalid user-supplied information.
 */
public class ValidationException extends DormRentalException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a descriptive error message.
     *
     * @param message explanation of the validation failure
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a message and underlying cause.
     *
     * @param message explanation of the validation failure
     * @param cause original exception that caused the failure
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
