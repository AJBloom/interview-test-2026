package com.ajbloom.interviewtests.model

// NOTE: Discussion — sealed class vs enum for domain events.
// Sealed classes allow each subtype to carry *different* data.
// The compiler enforces exhaustive `when` expressions:
// adding a new subtype is a COMPILE ERROR until every `when` branch handles it.
// Compare: an enum-based routing key would silently fall through at runtime.
//
// Discussion: what other event types belong here?
// StatusChanged? Fulfilled? Refunded? How granular should domain events be?
sealed class OrderEvent {
    abstract val order: Order

    data class Created(override val order: Order) : OrderEvent()
    data class Cancelled(override val order: Order) : OrderEvent()
}
