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

import com.arthurivanets.born2crawl.InputProcessor.Output.Companion.crawlableDataOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class CrawlerTests {

    @Test
    fun `invalid input handling`() {
        val crawler = Crawler(
            Crawler.TestConfig(
                inputProcessors = setOf(FakeInputProcessor()),
                resultStore = InMemoryCrawlingResultStore(),
                eventListener = TestCrawlerEventListener(),
                crawlingSessionFactory = ::TestCrawlingSession,
                coroutineDispatcher = UnconfinedTestDispatcher(),
            )
        )

        assertThrows<IllegalArgumentException> { crawler.submit("") }
        assertThrows<IllegalArgumentException> { crawler.submit("", "", "") }
        assertThrows<IllegalArgumentException> { crawler.submit("    ") }
        assertThrows<IllegalArgumentException> { crawler.submit("    ", "    ", "    ") }
    }

    @Test
    fun `handling of multiple initial inputs`() {
        val crawlingSessionFactory = TestCrawlingSessionFactory()

        val crawler = Crawler(
            Crawler.TestConfig(
                inputProcessors = setOf(FakeInputProcessor()),
                resultStore = InMemoryCrawlingResultStore(),
                eventListener = TestCrawlerEventListener(),
                crawlingSessionFactory = crawlingSessionFactory,
                coroutineDispatcher = UnconfinedTestDispatcher(),
                crawlingSessionParallelism = 3,
            )
        )

        crawler.submit("input1", "input2", "input3")

        assertEquals(1, crawlingSessionFactory.createdSessions.size)

        crawler.submit("input10", "input20")

        assertEquals(2, crawlingSessionFactory.createdSessions.size)
    }

    @Test
    fun `crawling session parallelism policy handling`() {
        val crawlingSessionFactory = TestCrawlingSessionFactory()
        val crawlingSessionParallelism = 2

        val crawler = Crawler(
            Crawler.TestConfig(
                inputProcessors = setOf(
                    FakeInputProcessor(
                        tagSuffix = "url",
                        inputProbingRegex = Regex(".*"),
                        output = listOf(
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=1"),
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=2"),
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=3"),
                        )
                    ),
                ),
                resultStore = InMemoryCrawlingResultStore(),
                eventListener = TestCrawlerEventListener(),
                crawlingSessionFactory = crawlingSessionFactory,
                coroutineDispatcher = UnconfinedTestDispatcher(),
                crawlingSessionParallelism = crawlingSessionParallelism,
            )
        )

        assertEquals(0, crawlingSessionFactory.createdSessions.size)

        // STAGE 1:
        // I: first batch of inputs
        // ER: a separate session for each input
        crawler.submit("www.yourwebsite.com/q?=something1")
        crawler.submit("www.yourwebsite.com/q?=something2")

        assertEquals(crawlingSessionParallelism, crawlingSessionFactory.createdSessions.size)

        // STAGE 2:
        // I: second batch of inputs
        // ER: no new sessions; new inputs must get enqueued for further processing
        crawler.submit("www.yourwebsite.com/q?=something3")
        crawler.submit("www.yourwebsite.com/q?=something4")

        assertEquals(crawlingSessionParallelism, crawlingSessionFactory.createdSessions.size)

        // STAGE 3:
        // I: first two sessions (currently active ones) successfully finish the crawling
        // ER: first two sessions get destroyed; new sessions get created from the previously enqueued inputs
        crawlingSessionFactory.createdSessions
            .take(2)
            .forEach(TestCrawlingSession::signalCrawlingFinished)

        assertEquals(4, crawlingSessionFactory.createdSessions.size)

        crawlingSessionFactory.createdSessions
            .take(2)
            .forEach { session -> assertEquals(TestCrawlingSession.State.DESTROYED, session.state) }

        crawlingSessionFactory.createdSessions
            .takeLast(2)
            .forEach { session -> assertEquals(TestCrawlingSession.State.INITIALIZED, session.state) }

        // STAGE 3:
        // I: first two sessions (currently active ones) successfully finish the crawling
        // ER: first two sessions get destroyed; new sessions get created from the previously enqueued inputs
        crawlingSessionFactory.createdSessions
            .takeLast(2)
            .forEach(TestCrawlingSession::signalCrawlingFinished)

        assertEquals(4, crawlingSessionFactory.createdSessions.size)

        crawlingSessionFactory.createdSessions.forEach { session ->
            assertEquals(TestCrawlingSession.State.DESTROYED, session.state)
        }
    }

    @Test
    fun `successful crawling`() {
        val eventListener = TestCrawlerEventListener()
        val crawlingSessionFactory = TestCrawlingSessionFactory()
        val crawlingSessionParallelism = 3

        val crawler = Crawler(
            Crawler.TestConfig(
                inputProcessors = setOf(
                    FakeInputProcessor(
                        tagSuffix = "url",
                        inputProbingRegex = Regex(".*"),
                        output = listOf(
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=1"),
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=2"),
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=3"),
                        )
                    ),
                ),
                resultStore = InMemoryCrawlingResultStore(),
                eventListener = eventListener,
                crawlingSessionFactory = crawlingSessionFactory,
                coroutineDispatcher = UnconfinedTestDispatcher(),
                crawlingSessionParallelism = crawlingSessionParallelism,
            )
        )

        crawler.submit("www.yourwebsite.com/q?=something1")
        crawler.submit("www.yourwebsite.com/q?=something2")
        crawler.submit("www.yourwebsite.com/q?=something3")

        assertEquals(crawlingSessionParallelism, crawlingSessionFactory.createdSessions.size)

        crawler.submit("www.yourwebsite.com/q?=something4")
        crawler.submit("www.yourwebsite.com/q?=something5")
        crawler.submit("www.yourwebsite.com/q?=something6")

        assertEquals(crawlingSessionParallelism, crawlingSessionFactory.createdSessions.size)

        crawlingSessionFactory.createdSessions
            .take(3)
            .forEach(TestCrawlingSession::signalCrawlingFinished)

        assertEquals(6, crawlingSessionFactory.createdSessions.size)

        crawlingSessionFactory.createdSessions
            .take(3)
            .forEach { session -> assertEquals(TestCrawlingSession.State.DESTROYED, session.state) }

        crawlingSessionFactory.createdSessions
            .takeLast(3)
            .forEach { session -> assertEquals(TestCrawlingSession.State.INITIALIZED, session.state) }

        crawlingSessionFactory.createdSessions
            .takeLast(3)
            .forEach(TestCrawlingSession::signalCrawlingFinished)

        assertEquals(6, crawlingSessionFactory.createdSessions.size)

        crawlingSessionFactory.createdSessions.forEach { session ->
            assertEquals(TestCrawlingSession.State.DESTROYED, session.state)
        }
    }

}