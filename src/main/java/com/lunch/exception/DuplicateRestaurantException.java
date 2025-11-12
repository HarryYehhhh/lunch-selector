package com.lunch.exception;

/**
 * 餐廳重複異常
 */
public class DuplicateRestaurantException extends RuntimeException {

    public DuplicateRestaurantException(String message) {
        super(message);
    }

    public DuplicateRestaurantException(String message, Throwable cause) {
        super(message, cause);
    }
}
