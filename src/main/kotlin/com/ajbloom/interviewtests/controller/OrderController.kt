package com.ajbloom.interviewtests.controller

import com.ajbloom.interviewtests.model.CreateOrderRequest
import com.ajbloom.interviewtests.model.Order
import com.ajbloom.interviewtests.model.OrderResponse
import com.ajbloom.interviewtests.model.OrderStatus
import com.ajbloom.interviewtests.service.OrderService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/orders")
class OrderController(private val orderService: OrderService) {

    @PostMapping
    fun createOrder(@RequestBody request: CreateOrderRequest): ResponseEntity<OrderResponse> {
        val order = orderService.createOrder(request)
        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(
                OrderResponse(
                    orderId = order.id,
                    status = order.status,
                    message = "Order accepted and queued for processing",
                )
            )
    }

    @GetMapping("/{id}")
    fun getOrder(@PathVariable id: String): ResponseEntity<Order> =
        ResponseEntity.ok(orderService.getOrder(id))

    @GetMapping
    fun listOrders(@RequestParam customerId: String?): ResponseEntity<List<Order>> {
        val orders = if (customerId != null) {
            orderService.getOrdersByCustomer(customerId)
        } else {
            orderService.getAllOrders()
        }
        return ResponseEntity.ok(orders)
    }

    @PostMapping("/{id}/cancel")
    fun cancelOrder(@PathVariable id: String): ResponseEntity<Order> =
        ResponseEntity.ok(orderService.cancelOrder(id))
}
