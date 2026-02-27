package com.ajbloom.interviewtests

import org.junit.jupiter.api.Test
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
class InterviewTest2026ApplicationTests {

    // Mock the connection factory so the context loads without a live RabbitMQ
    @MockitoBean
    lateinit var connectionFactory: ConnectionFactory

    @Test
    fun contextLoads() {
    }

}
