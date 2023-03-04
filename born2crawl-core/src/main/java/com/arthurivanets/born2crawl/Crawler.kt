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

import com.arthurivanets.born2crawl.util.logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * The crawling engine.
 *
 * NOTE: all public methods of the [Crawler] are thread-safe.
 */
class Crawler(private val config: Config) {

    private val log = logger()
    private val lock = ReentrantLock()
    private val pendingInputs = LinkedList<InitialInput>()
    private val activeSessions = mutableMapOf<String, CrawlingSession>()

    private var isShutdown = false

    /**
     * The config contract for the crawling engine.
     */
    interface Config {

        /**
         * Concrete implementations of the [InputProcessor] that are used by the [CrawlingSession]s to process
         * the available crawling data (i.e. [CrawlingInput]) and obtain new crawling data.
         *
         * NOTE: there MUST BE at least a single [InputProcessor] in order for the [Crawler] to be able to work.
         */
        val inputProcessors: Set<InputProcessor>

        /**
         * The store that is used by the [CrawlingSession]s to store the crawling results.
         */
        val resultStore: CrawlingResultStore

        /**
         * The maximum allowed number of concurrent crawling sessions.
         *
         * NOTE: if the maximum number of active crawling sessions is reached, all subsequent crawling requests will get enqueued
         * until the active sessions complete and it's possible to start new crawling sessions.
         */
        val crawlingSessionParallelism: Int

        /**
         * The maximum allowed number of crawling inputs to be submitted for concurrent processing by a single [CrawlingSession].
         *
         * NOTE: this property only defines the maximum level of concurrency for a single [CrawlingSession],
         * while the exact level of allowed input processing parallelism is ultimately dictated by the [coroutineDispatcher].
         */
        val inputProcessingBatchSize: Int

        /**
         * The maximum allowed depth of a tree of crawling outputs that a [CrawlingSession] is allowed to traverse.
         */
        val maxCrawlDepth: Int

        /**
         * The [CoroutineDispatcher] to be used by [CrawlingSession]s when spawning processing-specific coroutines.
         */
        val coroutineDispatcher: CoroutineDispatcher

        /**
         * The configuration for the throttling of the input processing requests initiated by the [CrawlingSession]s.
         * (See [ThrottlingConfig] for more details)
         */
        val throttlingConfig: ThrottlingConfig

        /**
         * The factory that is used for the creation of the [CrawlingSession]s for the submitted crawling requests.
         */
        val crawlingSessionFactory: ((CrawlingSession.Config) -> CrawlingSession)

        /**
         * The listener that allows to observe the crawling-specific events.
         * (See [Event] for more details)
         */
        val eventListener: ((Event) -> Unit)

    }

    /**
     * A set of events that can be emitted by the crawling engine.
     */
    sealed interface Event {

        /**
         * An event indicating that a crawling session completed successfully.
         *
         * @param crawlingInput the inputs (seeds) that were used to start a crawling session.
         * @param crawlingResultId the id of the stored [CrawlingResult] (the result can be retrieved from the [CrawlingResultStore]).
         * @param crawlingDuration the exact time it took complete the crawling session.
         */
        data class CrawlingFinished(
            val crawlingInput: Set<String>,
            val crawlingResultId: String,
            val crawlingDuration: CrawlingDuration,
        ) : Event

        /**
         * An event indicating that there was a critical failure during the crawling session that interrupted the session.
         *
         * @param crawlingInput the inputs (seeds) that were used to start a crawling session.
         * @param error the exact error that interrupted the crawling session.
         * @param crawlingDuration the exact duration of the crawling session.
         */
        data class CrawlingFailed(
            val crawlingInput: Set<String>,
            val error: Throwable,
            val crawlingDuration: CrawlingDuration,
        ) : Event

    }

    private class InitialInput(val inputs: Set<String>) {

        override fun toString(): String {
            return inputs.toString()
        }

    }

    companion object {

        /**
         * Creates a default [Config] for the crawling engine.
         *
         * @param inputProcessors concrete implementations of the [InputProcessor] to be used by the [CrawlingSession]s to process
         * the available crawling data (i.e. [CrawlingInput]) and obtain new crawling data.
         *
         * NOTE: there MUST BE at least a single [InputProcessor] in order for the [Crawler] to be able to work.
         *
         * @param resultStore the store that is used by the [CrawlingSession]s to store the crawling results.
         * @param eventListener the listener that allows to observe the crawling-specific [Event]s.
         *
         * @param crawlingSessionParallelism the maximum allowed number of concurrent crawling sessions.
         *
         * NOTE: if the maximum number of active crawling sessions is reached, all subsequent crawling requests will get enqueued
         * until the active sessions complete and it's possible to start new crawling sessions.
         *
         * @param inputProcessingBatchSize the maximum allowed number of crawling inputs to be submitted for concurrent processing by a single [CrawlingSession].
         * @param maxCrawlDepth the maximum allowed depth of a tree of crawling outputs that a [CrawlingSession] is allowed to traverse.
         * @param throttlingConfig The configuration for the throttling of the input processing requests initiated by the [CrawlingSession]s (See [ThrottlingConfig] for more details).
         *
         * @return the configuration for the [Crawler]
         */
        fun Config(
            inputProcessors: Set<InputProcessor>,
            resultStore: CrawlingResultStore,
            eventListener: ((Event) -> Unit),
            crawlingSessionParallelism: Int = 10,
            inputProcessingBatchSize: Int = 10,
            maxCrawlDepth: Int = Int.MAX_VALUE,
            throttlingConfig: ThrottlingConfig = ThrottlingConfig.NoOp,
        ): Config {
            require(inputProcessors.isNotEmpty()) { "Please, provide at least one InputProcessor" }
            require(crawlingSessionParallelism >= 1) { "CrawlingSession parallelism level cannot be lower than 1" }
            require(inputProcessingBatchSize >= 1) { "Please, provide a valid input processing batch size (MUST BE >= 1). Provided size: $inputProcessingBatchSize" }
            require(maxCrawlDepth >= 1) { "Please, provide a valid max crawl depth (MUST BE >= 1). Provided depth: $maxCrawlDepth" }

            return object : Config {
                override val inputProcessors = inputProcessors
                override val resultStore = resultStore
                override val crawlingSessionParallelism = crawlingSessionParallelism
                override val inputProcessingBatchSize = inputProcessingBatchSize
                override val maxCrawlDepth = maxCrawlDepth
                override val coroutineDispatcher = Dispatchers.IO
                override val throttlingConfig = throttlingConfig
                override val crawlingSessionFactory = ::CrawlingSessionImpl
                override val eventListener = eventListener
            }
        }

    }

    /**
     * Submits a crawling request to the crawling engine for further processing.
     *
     * Depending on the number of currently active crawling sessions, the request may either spawn a new session or get enqueued
     * until the corresponding resources get freed up.
     *
     * @param input the initial input (seed) to begin the crawling with.
     * @param extraInputs more initial inputs (seeds) to begin the crawling with.
     */
    fun submit(input: String, vararg extraInputs: String) = lock.withLock {
        check(!isShutdown) { "The Crawler has already been shutdown" }

        val initialInput = (listOf(input) + extraInputs.toList())
            .map { rawInput ->
                require(rawInput.isNotBlank()) { "Please, provide a valid non-empty input. Input = $rawInput" }
                normalizeInput(rawInput)
            }
            .toSet()
            .let(::InitialInput)

        if (activeSessions.size < config.crawlingSessionParallelism) {
            startCrawlingSession(initialInput)
        } else {
            log.info("The maximum number of active CrawlingSessions has been reached. Enqueuing the input(value = $initialInput) processing request.")

            pendingInputs.offer(initialInput)
        }
    }

    /**
     * Shuts down the crawling engine.
     *
     * NOTE: this is a terminal operation, which means that once a crawling engine is shutdown, it can no longer be used.
     */
    fun shutdown() = lock.withLock {
        if (!isShutdown) {
            log.info("Shutting down the Crawler...")

            pendingInputs.clear()
            ArrayList(activeSessions.keys).forEach(::destroyCrawlingSession)

            isShutdown = true
        }
    }

    private fun startCrawlingSession(input: InitialInput) {
        log.info("Starting a new CrawlingSession for the input: $input")

        val session = createCrawlingSession(input)
        session.init()

        activeSessions[session.sessionId] = session
    }

    private fun destroyCrawlingSession(sessionId: String) {
        activeSessions.remove(sessionId)?.let { session ->
            log.info("Destroying the CrawlingSession(id = $sessionId)...")
            session.destroy()
        }
    }

    private fun createCrawlingSession(input: InitialInput): CrawlingSession {
        return config.crawlingSessionFactory(
            CrawlingSession.Config(
                initialInputs = input.inputs,
                inputProcessingBatchSize = config.inputProcessingBatchSize,
                inputProcessors = config.inputProcessors,
                coroutineDispatcher = config.coroutineDispatcher,
                resultStore = config.resultStore,
                throttler = config.throttlingConfig.throttler(),
                maxCrawlDepth = config.maxCrawlDepth,
            )
        ).apply {
            eventListener = ::handleSessionEvent
        }
    }

    private fun handleSessionEvent(event: CrawlingSession.Event) {
        when (event) {
            is CrawlingSession.Event.CrawlingFinished -> {
                log.info("Crawling session finished for the initial input: [ ${event.initialInputs} ]. Session duration: ${event.crawlingDuration.duration}")

                destroyCrawlingSession(event.sessionId)
                dispatchEvent(
                    Event.CrawlingFinished(
                        crawlingInput = event.initialInputs,
                        crawlingResultId = event.resultId,
                        crawlingDuration = event.crawlingDuration,
                    )
                )
                processPendingInput()
            }
            is CrawlingSession.Event.CrawlingFailed -> {
                log.error("Crawling session failed for the initial input: [ ${event.initialInputs} ]. Session duration: ${event.crawlingDuration.duration}")

                destroyCrawlingSession(event.sessionId)
                dispatchEvent(
                    Event.CrawlingFailed(
                        crawlingInput = event.initialInputs,
                        error = event.error,
                        crawlingDuration = event.crawlingDuration,
                    )
                )
                processPendingInput()
            }
        }
    }

    private fun processPendingInput() {
        if (pendingInputs.isNotEmpty()) {
            val input = pendingInputs.poll()

            log.info("Consuming the pending input processing request. Input: $input")

            startCrawlingSession(input)
        }
    }

    private fun dispatchEvent(event: Event) {
        config.eventListener(event)
    }

    private fun normalizeInput(input: String): String {
        return input.trim()
    }

}