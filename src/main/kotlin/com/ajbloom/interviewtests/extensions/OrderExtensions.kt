package com.ajbloom.interviewtests.extensions

import com.ajbloom.interviewtests.model.Order
import com.ajbloom.interviewtests.model.OrderStatus

// NOTE: Discussion — extension functions vs member functions.
// Extension functions add behaviour to a type without modifying its source.
// Ask: when would you put this logic IN the class vs outside as an extension?
//
// Rules of thumb:
//   - Member function  → core invariant or state transition (e.g. Order.withStatus())
//   - Extension function → utility/query, operations on types you don't own,
//                          keeping data classes lean (no embedded business logic)
//
// Caveat: extension functions can't access private members — by design.

fun Order.isFinalState(): Boolean =
    status in setOf(OrderStatus.COMPLETED, OrderStatus.FAILED, OrderStatus.CANCELLED)

// NOTE: Discussion — computed property as extension vs field on data class.
// Adding totalItems to the data class would increase serialisation payload.
// As an extension it stays off the wire.
fun Order.totalItems(): Int = items.sumOf { it.quantity }

// NOTE: Discussion — extension on List vs a Repository query.
// This is fine for an in-memory store but would be O(n) against a DB.
// What would this look like as a @Query?
fun List<Order>.groupByStatus(): Map<OrderStatus, List<Order>> = groupBy { it.status }

fun List<Order>.pendingCount(): Int = count { it.status == OrderStatus.PENDING }

// ─── Value / inline classes ───────────────────────────────────────────────────
//
// NOTE: Discussion — value classes wrap a primitive at zero runtime cost (no boxing).
// They provide compile-time type safety: you can't pass a CustomerId where an
// OrderId is expected, even though both are plain Strings at runtime.
//
// In this codebase Order.customerId and Order.id are raw Strings — a common source
// of subtle bugs. These types show how you'd fix that in production.
@JvmInline
value class CustomerId(val value: String)

@JvmInline
value class OrderId(val value: String)
