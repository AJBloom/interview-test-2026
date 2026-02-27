package com.ajbloom.interviewtests.messaging

import com.ajbloom.interviewtests.config.RabbitMqConfig.Companion.ORDERS_EXCHANGE
import com.ajbloom.interviewtests.config.RabbitMqConfig.Companion.ROUTING_KEY_CANCELLED
import com.ajbloom.interviewtests.config.RabbitMqConfig.Companion.ROUTING_KEY_CREATED
import com.ajbloom.interviewtests.model.OrderEvent
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

@Component
class OrderMessagePublisher(
    private val rabbitTemplate: RabbitTemplate,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun publish(event: OrderEvent) {
        // NOTE: Discussion — exhaustive `when` over a sealed class.
        // Adding a new OrderEvent subtype is a COMPILE ERROR until this when is updated.
        // Contrast with a String/enum routing key: the compiler can't help you there.
        val routingKey = when (event) {
            is OrderEvent.Created -> ROUTING_KEY_CREATED
            is OrderEvent.Cancelled -> ROUTING_KEY_CANCELLED
        }

        // NOTE: Discussion — what happens if RabbitMQ is unavailable here?
        // The order is already saved; we'd have a ghost order stuck in PENDING.
        // Solutions: outbox pattern, publisher confirms, transactional messaging.
        rabbitTemplate.convertAndSend(ORDERS_EXCHANGE, routingKey, event.order)
        logger.info("Published ${event::class.simpleName} for order: ${event.order.id}")
    }
}
