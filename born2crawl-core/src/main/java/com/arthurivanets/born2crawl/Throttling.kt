/*
 * Copyright 2022 Arthur Ivanets, arthur.ivanets.work@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arthurivanets.born2crawl

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration

/**
 * A set of crawling speed throttling configurations.
 */
sealed interface ThrottlingConfig {

    /**
     * No throttling will be applied.
     */
    object NoOp : ThrottlingConfig {
        override fun throttler(): Throttler = NoThrottling
    }

    /**
     * Every input processing request will be throttled by pausing the processing by a specified fixed [delay].
     */
    class FixedDelay(private val delay: Duration) : ThrottlingConfig {
        override fun throttler(): Throttler = FixedDelayThrottler(delay)
    }

    /**
     * A customizable throttling configuration that allows to specify which [InputProcessor]s must be throttled.
     * (Allows to specify custom throttling delays for different [InputProcessor]s)
     */
    class PerProcessor(vararg delays: Pair<Class<out InputProcessor>, Duration>) : ThrottlingConfig {

        private val delaysPerProcessor = delays.toMap()

        companion object {

            /**
             * Creates a throttling rule for the specified [InputProcessor].
             */
            @JvmStatic
            inline fun <reified T : InputProcessor> delay(delay: Duration): Pair<Class<out InputProcessor>, Duration> {
                return (T::class.java to delay)
            }

        }

        override fun throttler(): Throttler = ConfigurableThrottler(
            delaysPerProcessor.mapValues { (_, delay) -> FixedDelayThrottler(delay) },
            NoThrottling
        )

    }

    fun throttler(): Throttler

}

internal class ConfigurableThrottler(
    private val throttlersByInputProcessor: Map<Class<out InputProcessor>, Throttler>,
    private val fallbackThrottler: Throttler,
) : Throttler {

    override suspend fun throttle(processor: InputProcessor) {
        (throttlersByInputProcessor[processor::class.java] ?: fallbackThrottler).throttle(processor)
    }

}

internal class FixedDelayThrottler(private val delay: Duration) : Throttler {

    private val throttlingEndTimeByProcessor = mutableMapOf<Class<out InputProcessor>, Long>()
    private val mutex = Mutex()

    override suspend fun throttle(processor: InputProcessor) {
        val throttlingDelay = mutex.withLock {
            val processorKey = processor::class.java
            val previousEndTimeMs = (throttlingEndTimeByProcessor[processorKey] ?: System.currentTimeMillis())
            val newEndTimeMs = (previousEndTimeMs + delay.toMillis())
            throttlingEndTimeByProcessor[processorKey] = newEndTimeMs

            (newEndTimeMs - System.currentTimeMillis())
        }

        delay(throttlingDelay)
    }

}

internal object NoThrottling : Throttler {

    override suspend fun throttle(processor: InputProcessor) = Unit

}

/**
 * A contract for the implementation of concrete [InputProcessor]-specific throttlers.
 */
interface Throttler {

    /**
     * Throttles the processing for the specified [processor].
     *
     * NOTE: this method MUST NEVER throw exceptions.
     */
    suspend fun throttle(processor: InputProcessor)

}
