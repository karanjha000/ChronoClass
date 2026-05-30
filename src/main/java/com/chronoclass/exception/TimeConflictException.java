/*
 * Copyright (c) 2026 ChronoClass. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.exception;

import lombok.Getter;

import java.util.List;

/**
 * Thrown when a booking would create a time conflict with existing bookings.
 */
@Getter
public class TimeConflictException extends RuntimeException {

    private final List<String> conflicts;

    public TimeConflictException(String message, List<String> conflicts) {
        super(message);
        this.conflicts = conflicts;
    }

    public TimeConflictException(String message) {
        super(message);
        this.conflicts = List.of();
    }
}
