package com.bryant.denden_homework.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** Generates 6-digit one-time codes. A bean so tests can substitute a fixed value. */
@Component
public class OtpGenerator {

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
