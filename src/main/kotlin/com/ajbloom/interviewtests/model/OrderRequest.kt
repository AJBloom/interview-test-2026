package com.ajbloom.interviewtests.model

import java.math.BigDecimal

data class CreateOrderRequest(
    val customerId: String,
    val items: List<OrderItem>,
) {
    // NOTE: totalAmount is computed from line items — intentional discussion point:
    // should the caller send a total, or do we always calculate server-side?
    val totalAmount: BigDecimal get() = items.sumOf { it.lineTotal }
}

data class OrderResponse(
    val orderId: String,
    val status: OrderStatus,
    val message: String,
)
