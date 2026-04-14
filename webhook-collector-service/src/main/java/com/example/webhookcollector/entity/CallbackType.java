package com.example.webhookcollector.entity;

public enum CallbackType {

    PAYMENT_STATUS_UPDATED,
    PAYMENT_FAILED,
    PAYMENT_REFUNDED;

    public static boolean isKnown(String type) {
        if (type == null) {
            return false;
        }
        for (CallbackType value : values()) {
            if (value.name().equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }
}