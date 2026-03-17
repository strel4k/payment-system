package com.example.payment.service;

public record WebhookNotification(String type, Long id, String status) {
}