package com.ajbloom.interviewtests.repository

import com.ajbloom.interviewtests.model.Order
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class OrderRepository {

    // NOTE: In-memory store — fine for demo. Production would use a database.
    // Discussion point: ConcurrentHashMap makes individual put/get thread-safe,
    // but a multi-step read-then-write (e.g. cancel) is NOT atomic.
    private val store = ConcurrentHashMap<String, Order>()

    fun save(order: Order): Order {
        store[order.id] = order
        return order
    }

    fun findById(id: String): Order? = store[id]

    fun findAll(): List<Order> = store.values.toList()

    fun findByCustomerId(customerId: String): List<Order> =
        store.values.filter { it.customerId == customerId }
}
