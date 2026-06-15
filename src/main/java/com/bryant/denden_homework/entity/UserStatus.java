package com.bryant.denden_homework.entity;

/**
 * Member account lifecycle status.
 * PENDING: registered but email not yet verified (cannot log in).
 * ACTIVE: email verified, account usable.
 */
public enum UserStatus {
    PENDING,
    ACTIVE
}
