package com.ajbloom.interviewtests.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.core.task.SimpleAsyncTaskExecutor

@Configuration
class VirtualThreadConfig {

    // NOTE: Discussion — Spring Boot 3.2+ enables virtual threads globally via ONE property:
    //   spring.threads.virtual.enabled=true   (see application.properties)
    // That replaces BOTH the Tomcat request thread pool AND @Async executor with virtual threads.
    //
    // This explicit bean shows what Spring Boot does under the hood and gives
    // fine-grained control, e.g. a dedicated executor for specific @Async tasks.
    //
    // ─── Virtual threads vs Kotlin coroutines — when to use which? ────────────
    //
    // Virtual threads (Project Loom):
    //   ✅ Zero code changes to existing blocking code (no suspend funs required)
    //   ✅ Great "lift and shift" for legacy codebases
    //   ✅ Thousands blocking on I/O = cheap (JVM unmounts the carrier thread)
    //   ⚠️  Pinning hazard: synchronized blocks inside a virtual thread pin the
    //       carrier platform thread, defeating the purpose. Use ReentrantLock instead.
    //   ⚠️  CPU-bound work doesn't benefit — virtual threads don't add parallelism
    //
    // Kotlin coroutines:
    //   ✅ Structured concurrency (scope, cancellation, parent-child lifetime)
    //   ✅ Backpressure via Flow, composability via suspend/async/await
    //   ✅ First-class support in Spring WebFlux and R2DBC
    //   ⚠️  Requires suspend fun adoption throughout the call stack
    //   ⚠️  More conceptual overhead (dispatchers, contexts, scopes)
    //
    // Discussion: does replacing Thread.sleep(500) in OrderMessageConsumer
    // with a virtual thread actually solve the problem?
    // (Hint: virtual threads unmount during sleep, so Tomcat/listener threads
    //  are freed — but the order still takes ≥500 ms to complete.)
    @Bean("virtualThreadExecutor")
    fun virtualThreadExecutor(): AsyncTaskExecutor =
        SimpleAsyncTaskExecutor("vt-").also { it.setVirtualThreads(true) }
}
