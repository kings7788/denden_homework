package com.bryant.denden_homework.entity;

/**
 * Purpose of a VerificationToken.
 * ACTIVATION: one-time link to activate a newly registered account.
 * LOGIN_OTP: one-time code for the second factor during login.
 */
public enum TokenType {
    ACTIVATION,
    LOGIN_OTP
}
