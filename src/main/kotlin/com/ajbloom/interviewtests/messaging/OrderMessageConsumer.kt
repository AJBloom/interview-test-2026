package com.ajbloom.interviewtests.messaging

import com.ajbloom.interviewtests.config.RabbitMqConfig.Companion.ORDERS_QUEUE
import com.ajbloom.interviewtests.model.Order
import com.ajbloom.interviewtests.service.OrderService
import com.rabbitmq.client.Channel
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.support.AmqpHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
class OrderMessageConsumer(
    private val orderService: OrderService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // containerFactory refers to the MANUAL-ack factory in RabbitMqConfig
    @RabbitListener(queues = [ORDERS_QUEUE], containerFactory = "rabbitListenerContainerFactory")
    fun handleOrderCreated(
        order: Order,
        channel: Channel,
        @Header(AmqpHeaders.DELIVERY_TAG) deliveryTag: Long,
    ) {
        logger.info("Received order for processing: ${order.id}")

        try {
            orderService.markProcessing(order.id)

            // NOTE: Discussion — virtual threads and blocking I/O.
            // With spring.threads.virtual.enabled=true, this Thread.sleep() UNMOUNTS
            // the virtual thread from its carrier platform thread, so the carrier is
            // freed to run other virtual threads. Throughput improves without any
            // code changes — that's the virtual thread promise.
            //
            // However, the order still takes ≥500 ms to complete. Virtual threads
            // increase CONCURRENCY (more tasks in flight) but not LATENCY (each task
            // is no faster). The real fix is to move the processing off the listener
            // thread entirely — e.g. via @Async, a WorkflowEngine, or coroutines.
            //
            // Pinning hazard: if this code held a synchronized lock during the sleep,
            // the carrier platform thread would be pinned — negating all the benefit.
            // Prefer ReentrantLock over synchronized when using virtual threads.
            Thread.sleep(500)

            // NOTE: Discussion point — no idempotency guard.
            // If this message is redelivered (e.g. after a crash between ack and DB write),
            // we'd process the same order twice.
            orderService.markCompleted(order.id)

            channel.basicAck(deliveryTag, false)
            logger.info("Order processed and acked: ${order.id}")
        } catch (ex: Exception) {
            logger.error("Failed to process order: ${order.id} — nacking to DLQ", ex)
            orderService.markFailed(order.id)
            // NOTE: requeue = false → message routed to DLQ via x-dead-letter-routing-key
            channel.basicNack(deliveryTag, false, false)
        }
    }
}
