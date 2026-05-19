package com.platform.payment.config;

public final class TopicNames {
    public static final String ORDER_CREATED = "incidents.v1.order.created";
    public static final String ORDER_CREATED_DLT = ORDER_CREATED + ".dlt";
    public static final String PAYMENT_COMPLETED = "incidents.v1.payment.completed";
    public static final String PAYMENT_FAILED = "incidents.v1.payment.failed";
    public static final String NOTIFICATION_TRIGGERED = "incidents.v1.notification.triggered";

    private TopicNames() {
    }
}
