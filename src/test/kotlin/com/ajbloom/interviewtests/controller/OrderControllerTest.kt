package com.ajbloom.interviewtests.controller

import com.ajbloom.interviewtests.exception.OrderNotFoundException
import com.ajbloom.interviewtests.exception.OrderStateException
import com.ajbloom.interviewtests.model.CreateOrderRequest
import com.ajbloom.interviewtests.model.Order
import com.ajbloom.interviewtests.model.OrderItem
import com.ajbloom.interviewtests.model.OrderStatus
import com.ajbloom.interviewtests.service.OrderService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
class OrderControllerTest {

    private val orderService: OrderService = mock()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(OrderController(orderService))
            .setControllerAdvice(GlobalExceptionHandler())
            .setMessageConverters(MappingJackson2HttpMessageConverter())
            .build()
    }

    private val sampleOrder = Order(
        id = "order-123",
        customerId = "cust-1",
        items = listOf(
            OrderItem(productId = "PROD-1", quantity = 1, unitPrice = BigDecimal("25.00"))
        ),
        totalAmount = BigDecimal("25.00"),
        status = OrderStatus.PENDING,
    )

    @Test
    fun `POST createOrder returns 202 Accepted`() {
        whenever(orderService.createOrder(any<CreateOrderRequest>())).thenReturn(sampleOrder)

        mockMvc.post("/api/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "customerId": "cust-1",
                  "items": [{ "productId": "PROD-1", "quantity": 1, "unitPrice": 25.00 }]
                }
            """.trimIndent()
        }.andExpect {
            status { isAccepted() }
            jsonPath("$.orderId") { value("order-123") }
            jsonPath("$.status") { value("PENDING") }
        }
    }

    @Test
    fun `GET getOrder returns order`() {
        whenever(orderService.getOrder("order-123")).thenReturn(sampleOrder)

        mockMvc.get("/api/orders/order-123").andExpect {
            status { isOk() }
            jsonPath("$.id") { value("order-123") }
            jsonPath("$.customerId") { value("cust-1") }
            jsonPath("$.status") { value("PENDING") }
        }
    }

    @Test
    fun `GET getOrder returns 404 when not found`() {
        whenever(orderService.getOrder("missing")).thenThrow(OrderNotFoundException("missing"))

        mockMvc.get("/api/orders/missing").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `POST cancelOrder returns 409 when order is not cancellable`() {
        whenever(orderService.cancelOrder("order-123"))
            .thenThrow(OrderStateException("Cannot cancel from PROCESSING"))

        mockMvc.post("/api/orders/order-123/cancel").andExpect {
            status { isConflict() }
        }
    }
}
