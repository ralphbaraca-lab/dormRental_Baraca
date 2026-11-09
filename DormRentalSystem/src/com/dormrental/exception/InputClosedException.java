package com.dormrental.exception;

/** Raised when standard input ends (Ctrl+D / Ctrl+Z or piped input runs out). */
public class InputClosedException extends DormRentalException {
    private static final long serialVersionUID = 1L;

    public InputClosedException() {
        super("Input stream closed.");
    }
}
