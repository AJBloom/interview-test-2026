package com.ajbloom.interviewtests.config

import org.springframework.amqp.core.AcknowledgeMode
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMqConfig {
    companion object {
        const val ORDERS_EXCHANGE = "orders.exchange"
        const val ORDERS_QUEUE = "orders.queue"
        const val ORDERS_DLQ = "orders.dlq"
        const val ROUTING_KEY_CREATED = "order.created"
        const val ROUTING_KEY_CANCELLED = "order.cancelled"
    }

    @Bean
    fun ordersExchange(): TopicExchange = TopicExchange(ORDERS_EXCHANGE)

    // NOTE: Discussion point — why a topic exchange over direct/fanout?
    // Topic gives routing flexibility: e.g. future "order.#" wildcard consumers.
    @Bean
    fun ordersQueue(): Queue =
        QueueBuilder
            .durable(ORDERS_QUEUE)
            .withArgument("x-dead-letter-exchange", "")
            .withArgument("x-dead-letter-routing-key", ORDERS_DLQ)
            .build()

    @Bean
    fun deadLetterQueue(): Queue = QueueBuilder.durable(ORDERS_DLQ).build()

    @Bean
    fun ordersBinding(
        ordersQueue: Queue,
        ordersExchange: TopicExchange,
    ): Binding = BindingBuilder.bind(ordersQueue).to(ordersExchange).with(ROUTING_KEY_CREATED)

    @Bean
    fun messageConverter(): Jackson2JsonMessageConverter = Jackson2JsonMessageConverter()

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory): RabbitTemplate =
        RabbitTemplate(connectionFactory).also {
            it.messageConverter = messageConverter()
        }

    // NOTE: MANUAL ack — consumer must explicitly ack/nack each message.
    // Discussion point: what happens if the consumer crashes before ack?
    @Bean
    fun rabbitListenerContainerFactory(connectionFactory: ConnectionFactory): SimpleRabbitListenerContainerFactory =
        SimpleRabbitListenerContainerFactory().also {
            it.setConnectionFactory(connectionFactory)
            it.setMessageConverter(messageConverter())
            it.setAcknowledgeMode(AcknowledgeMode.MANUAL)
        }
}
