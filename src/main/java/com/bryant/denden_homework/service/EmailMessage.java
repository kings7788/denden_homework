package com.bryant.denden_homework.service;

/** A ready-to-send email (transport-agnostic). Built by {@link EmailContentFactory}. */
public record EmailMessage(String subject, String textBody, String htmlBody) {
}
