package com.ecommerce.sportcenter.exception;

/**
 * Thrown on business-rule violation. Handled as 400.
 */
public class BusinessValidationException extends RuntimeException {
    public BusinessValidationException(String message) {
        super(message);
    }
}
