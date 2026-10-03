package com.ecommerce.sportcenter.exception;

/**
 * Thrown when a requested resource does not exist. Handled as 404.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
