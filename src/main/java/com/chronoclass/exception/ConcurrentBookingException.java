/*
 * Copyright (c) 2026 Karan Jha. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.exception;

public class ConcurrentBookingException extends RuntimeException {

    public ConcurrentBookingException(String message) {
        super(message);
    }
}
