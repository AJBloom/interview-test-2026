package com.ajbloom.interviewtests.service

import com.ajbloom.interviewtests.exception.OrderNotFoundException
import com.ajbloom.interviewtests.exception.OrderStateException
import com.ajbloom.interviewtests.messaging.OrderMessagePublisher
import com.ajbloom.interviewtests.model.CreateOrderRequest
import com.ajbloom.interviewtests.model.Order
import com.ajbloom.interviewtests.model.OrderItem
import com.ajbloom.interviewtests.model.OrderEvent
import com.ajbloom.interviewtests.model.OrderStatus
import com.ajbloom.interviewtests.repository.OrderRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class OrderServiceTest {

    private val repository: OrderRepository = mock()
    private val publisher: OrderMessagePublisher = mock()
    private val service = OrderService(repository, publisher)

    private val sampleItem = OrderItem(
        productId = "PROD-001",
        quantity = 2,
        unitPrice = BigDecimal("9.99"),
    )
    private val sampleRequest = CreateOrderRequest(
        customerId = "CUST-42",
        items = listOf(sampleItem),
    )

    @BeforeEach
    fun setUp() {
        whenever(repository.save(any())).thenAnswer { it.arguments[0] as Order }
    }

    @Test
    fun `createOrder saves order and publishes event`() {
        val order = service.createOrder(sampleRequest)

        val orderCaptor = argumentCaptor<Order>()
        verify(repository).save(orderCaptor.capture())

        val eventCaptor = argumentCaptor<OrderEvent>()
        verify(publisher).publish(eventCaptor.capture())
        assertInstanceOf(OrderEvent.Created::class.java, eventCaptor.firstValue)

        assertEquals(OrderStatus.PENDING, order.status)
        assertEquals("CUST-42", order.customerId)
        assertEquals(BigDecimal("19.98"), order.totalAmount)
    }

    @Test
    fun `getOrder throws when not found`() {
        whenever(repository.findById("missing")).thenReturn(null)

        assertThrows(OrderNotFoundException::class.java) {
            service.getOrder("missing")
        }
    }

    @Test
    fun `cancelOrder transitions PENDING order to CANCELLED`() {
        val pending = Order(
            customerId = "CUST-42",
            items = listOf(sampleItem),
            totalAmount = BigDecimal("19.98"),
        )
        whenever(repository.findById(pending.id)).thenReturn(pending)

        val cancelled = service.cancelOrder(pending.id)

        assertEquals(OrderStatus.CANCELLED, cancelled.status)
    }

    @Test
    fun `cancelOrder throws when order is already PROCESSING`() {
        val processing = Order(
            customerId = "CUST-42",
            items = listOf(sampleItem),
            totalAmount = BigDecimal("19.98"),
            status = OrderStatus.PROCESSING,
        )
        whenever(repository.findById(processing.id)).thenReturn(processing)

        assertThrows(OrderStateException::class.java) {
            service.cancelOrder(processing.id)
        }
        verify(repository, never()).save(any())
    }

    @Test
    fun `markCompleted updates order status to COMPLETED`() {
        val order = Order(
            customerId = "CUST-42",
            items = listOf(sampleItem),
            totalAmount = BigDecimal("19.98"),
            status = OrderStatus.PROCESSING,
        )
        whenever(repository.findById(order.id)).thenReturn(order)

        service.markCompleted(order.id)

        val captor = argumentCaptor<Order>()
        verify(repository).save(captor.capture())
        assertEquals(OrderStatus.COMPLETED, captor.firstValue.status)
    }
}
