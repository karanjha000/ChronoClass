/*
 * Copyright (c) 2026 ChronoClass. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.exception;

/**
 * Thrown when a concurrent booking attempt fails due to capacity or race condition.
 */
public class ConcurrentBookingException extends RuntimeException {

    public ConcurrentBookingException(String message) {
        super(message);
    }
}
