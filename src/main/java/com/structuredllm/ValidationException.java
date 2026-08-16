package com.structuredllm;

/**
 * Custom exception thrown when LLM output extraction or
 * record field validation fails.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
