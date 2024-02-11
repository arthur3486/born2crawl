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

import kotlinx.coroutines.CoroutineDispatcher

/**
 * A contract for the implementation of concrete crawling sessions.
 *
 * Crawling sessions are responsible for the exhaustive processing of the crawling requests.
 * Every crawling request results in a separate crawling session.
 */
interface CrawlingSession {

    /**
     * Session configuration.
     */
    class Config(
        val initialInputs: Set<String>,
        val inputProcessingBatchSize: Int,
        val inputProcessors: Set<InputProcessor>,
        val coroutineDispatcher: CoroutineDispatcher,
        val resultStore: CrawlingResultStore,
        val traversalAlgorithm: TraversalAlgorithm = TraversalAlgorithm.BFS,
        val throttler: Throttler = NoThrottling,
        val maxCrawlDepth: Int = Int.MAX_VALUE,
    )

    /**
     * A set of events that can be emitted by the [CrawlingSession].
     */
    sealed interface Event {

        /**
         * An event indicating that a crawling session has started.
         *
         * @param sessionId the id of the session that emitted the event.
         * @param initialInputs the inputs (seeds) that were used to start a crawling session.
         */
        class CrawlingStarted(
            val sessionId: String,
            val initialInputs: Set<String>,
        ) : Event

        /**
         * An event indicating that a crawling session completed successfully.
         *
         * @param resultId the id of the stored [CrawlingResult] (the result can be retrieved from the [CrawlingResultStore]).
         * @param sessionId the id of the session that emitted the event.
         * @param initialInputs the inputs (seeds) that were used to start a crawling session.
         * @param crawlingDuration the exact time it took complete the crawling session.
         */
        class CrawlingFinished(
            val resultId: String,
            val sessionId: String,
            val initialInputs: Set<String>,
            val crawlingDuration: CrawlingDuration,
        ) : Event

        /**
         * An event indicating that there was a critical failure during the crawling session that interrupted the session.
         *
         * @param error the exact error that interrupted the crawling session.
         * @param sessionId the id of the session that emitted the event.
         * @param initialInputs the inputs (seeds) that were used to start a crawling session.
         * @param crawlingDuration the exact duration of the crawling session.
         */
        class CrawlingFailed(
            val error: Throwable,
            val sessionId: String,
            val initialInputs: Set<String>,
            val crawlingDuration: CrawlingDuration,
        ) : Event

    }

    /**
     * A unique identifier of the current session.
     */
    val sessionId: String

    /**
     * The listener that allows to observe the crawling-specific events.
     * (See [Event] for more details)
     */
    var eventListener: ((Event) -> Unit)?

    /**
     * Initializes the session (begins the crawling).
     */
    fun init()

    /**
     * Destroys the session (releases all held resources).
     */
    fun destroy()

}
