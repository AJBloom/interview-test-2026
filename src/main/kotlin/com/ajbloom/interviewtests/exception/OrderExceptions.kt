package com.ajbloom.interviewtests.exception

class OrderNotFoundException(id: String) :
    RuntimeException("Order not found: $id")

class OrderStateException(message: String) :
    RuntimeException(message)
