/*
 * Copyright (c) 2026 Karan Jha. All rights reserved.
 * This software is submitted for evaluation purposes only.
 * Unauthorized commercial use, reproduction, or distribution is prohibited.
 */
package com.chronoclass.exception;

import lombok.Getter;

import java.util.List;

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
