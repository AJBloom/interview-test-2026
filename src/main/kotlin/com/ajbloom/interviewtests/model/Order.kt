package com.ajbloom.interviewtests.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class OrderStatus {
    PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
}

data class OrderItem(
    val productId: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
) {
    val lineTotal: BigDecimal get() = unitPrice.multiply(quantity.toBigDecimal())
}

data class Order(
    val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val items: List<OrderItem>,
    val totalAmount: BigDecimal,
    val status: OrderStatus = OrderStatus.PENDING,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    val isCancellable: Boolean get() = status == OrderStatus.PENDING

    fun withStatus(newStatus: OrderStatus): Order = copy(
        status = newStatus,
        updatedAt = Instant.now(),
    )
}
