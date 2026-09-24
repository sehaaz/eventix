package com.sehaaz.eventtix.common.messaging;

public final class SagaMessaging {

    public static final String EXCHANGE = "eventtix.exchange";
    public static final String DLX = "eventtix.dlx";
    public static final String DLQ_SUFFIX = ".dlq";

    public static final String ORDER_CREATED = "order.created";
    public static final String QUOTA_RESERVED = "quota.reserved";
    public static final String QUOTA_REJECTED = "quota.rejected";
    public static final String ORDER_CONFIRMED = "order.confirmed";
    public static final String TICKET_GENERATED = "ticket.generated";
    public static final String TICKET_FAILED = "ticket.failed";
    public static final String ORDER_CANCELLED = "order.cancelled";
    public static final String ORDER_COMPLETED = "order.completed";
    public static final String ORDER_FAILED = "order.failed";

    public static final String EVENT_QUOTA_QUEUE = "event.quota.queue";
    public static final String ORDER_QUOTA_RESERVED_QUEUE = "order.quota-reserved.queue";
    public static final String ORDER_QUOTA_REJECTED_QUEUE = "order.quota-rejected.queue";
    public static final String TICKET_GENERATE_QUEUE = "ticket.generate.queue";
    public static final String ORDER_TICKET_GENERATED_QUEUE = "order.ticket-generated.queue";
    public static final String ORDER_TICKET_FAILED_QUEUE = "order.ticket-failed.queue";
    public static final String EVENT_QUOTA_RELEASE_QUEUE = "event.quota-release.queue";
    public static final String NOTIFICATION_SUCCESS_QUEUE = "notification.success.queue";
    public static final String NOTIFICATION_FAILURE_QUEUE = "notification.failure.queue";

    private SagaMessaging() {
    }
}
