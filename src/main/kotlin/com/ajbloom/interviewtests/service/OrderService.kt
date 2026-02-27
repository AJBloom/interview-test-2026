package com.ajbloom.interviewtests.service

import com.ajbloom.interviewtests.exception.OrderNotFoundException
import com.ajbloom.interviewtests.exception.OrderStateException
import com.ajbloom.interviewtests.extensions.isFinalState
import com.ajbloom.interviewtests.messaging.OrderMessagePublisher
import com.ajbloom.interviewtests.model.CreateOrderRequest
import com.ajbloom.interviewtests.model.Order
import com.ajbloom.interviewtests.model.OrderEvent
import com.ajbloom.interviewtests.model.OrderStatus
import com.ajbloom.interviewtests.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OrderService(
    private val repository: OrderRepository,
    private val publisher: OrderMessagePublisher,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun createOrder(request: CreateOrderRequest): Order {
        val order = Order(
            customerId = request.customerId,
            items = request.items,
            totalAmount = request.totalAmount,
        )
        repository.save(order)
        // NOTE: Discussion — scope functions. `also` runs a side-effect and returns the receiver.
        // Contrast with `let` (transforms), `run` (executes block, returns result),
        // `apply` (configures an object, returns it), `with` (non-extension form of run).
        publisher.publish(OrderEvent.Created(order))
        logger.info("Order created and published: ${order.id}")
        return order
    }

    fun getOrder(id: String): Order =
        repository.findById(id) ?: throw OrderNotFoundException(id)

    fun getOrdersByCustomer(customerId: String): List<Order> =
        repository.findByCustomerId(customerId)

    fun getAllOrders(): List<Order> = repository.findAll()

    fun cancelOrder(id: String): Order {
        val order = repository.findById(id) ?: throw OrderNotFoundException(id)

        // NOTE: Discussion — this check + save is NOT atomic.
        // A race condition exists if two cancel requests arrive simultaneously.
        if (!order.isCancellable) {
            throw OrderStateException(
                "Order ${order.id} cannot be cancelled from status '${order.status}'"
            )
        }

        return repository.save(order.withStatus(OrderStatus.CANCELLED)).also {
            publisher.publish(OrderEvent.Cancelled(it))
        }
    }

    // Called by the RabbitMQ consumer — keep internal visibility
    internal fun markProcessing(id: String) = updateStatus(id, OrderStatus.PROCESSING)

    internal fun markCompleted(id: String) = updateStatus(id, OrderStatus.COMPLETED)

    internal fun markFailed(id: String) = updateStatus(id, OrderStatus.FAILED)

    private fun updateStatus(id: String, status: OrderStatus): Order {
        val order = repository.findById(id) ?: throw OrderNotFoundException(id)

        // NOTE: Discussion — extension function `isFinalState()` used as a guard.
        // Ask: should this guard throw an exception, silently return, or log a warning?
        // Also: who is responsible for knowing an order is in a final state —
        // the service, the repository, or the domain model itself?
        if (order.isFinalState()) {
            logger.warn("Attempted to transition already-final order $id to $status — skipping")
            return order
        }

        return repository.save(order.withStatus(status)).also {
            logger.info("Order $id transitioned to $status")
        }
    }
}
