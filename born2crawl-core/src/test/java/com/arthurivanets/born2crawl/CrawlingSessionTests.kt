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

import com.arthurivanets.born2crawl.CrawlingSession.Event
import com.arthurivanets.born2crawl.InputProcessor.Output.Companion.crawlableDataOf
import com.arthurivanets.born2crawl.InputProcessor.Output.Companion.uncrawlableDataOf
import com.arthurivanets.born2crawl.InputProcessor.ValueHolder.Companion.asCrawlable
import com.arthurivanets.born2crawl.InputProcessor.ValueHolder.Companion.asUncrawlable
import com.arthurivanets.born2crawl.util.UniqueIds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.io.IOException

internal class CrawlingSessionTests {

    @Test
    fun `successful exhaustive traversal - complex traversal with input cycles, no max crawling depth limit`() {
        // [ simulation of user profile info fetching ]
        // input(phone_number) -> output(full_name, email_address)
        // input(full_name) -> output(website(-s))
        // input(website) -> output(full_name, phone_number)
        // input(email_address) -> output(username)
        // input(username) -> output(email_address, full_name)
        // [ full stop ]

        val resultStore = InMemoryCrawlingResultStore()
        val eventListener = TestCrawlingSessionEventListener()

        val keyFullName = "full_name"
        val keyPhoneNumber = "phone_number"
        val keyWebsite = "website"
        val keyEmailAddress = "email_address"
        val keyUsername = "username"

        val fakePhoneNumber = "+1-555-5555"
        val fakeFullName = "John Smith"
        val fakeEmailAddress = "john.smith.1b@example.com"
        val fakeWebsite1 = "www.johnsmith123.com"
        val fakeWebsite2 = "www.johnsmith.net"
        val fakeUsername = "johny123"

        val session = CrawlingSessionImpl(
            CrawlingSession.Config(
                initialInputs = setOf(fakePhoneNumber),
                inputProcessingBatchSize = 10,
                inputProcessors = setOf(
                    FakeInputProcessor(
                        tagSuffix = "full-name",
                        inputProbingRegex = Regex("^(${Regex.escape(fakePhoneNumber)})$"),
                        output = listOf(crawlableDataOf(keyFullName to fakeFullName))
                    ),
                    FakeInputProcessor(
                        tagSuffix = "email-address",
                        inputProbingRegex = Regex("^(${Regex.escape(fakePhoneNumber)})$"),
                        output = listOf(crawlableDataOf(keyEmailAddress to fakeEmailAddress))
                    ),

                    FakeInputProcessor(
                        tagSuffix = "website",
                        inputProbingRegex = Regex("^(${Regex.escape(fakeFullName)})$"),
                        output = listOf(
                            crawlableDataOf(keyWebsite to fakeWebsite1),
                            crawlableDataOf(keyWebsite to fakeWebsite2),
                        )
                    ),

                    FakeInputProcessor(
                        tagSuffix = "brief-info-by-website",
                        inputProbingRegex = Regex("^(${Regex.escape(fakeWebsite1)})$"),
                        output = listOf(crawlableDataOf(keyFullName to fakeFullName, keyPhoneNumber to fakePhoneNumber))
                    ),

                    FakeInputProcessor(
                        tagSuffix = "username",
                        inputProbingRegex = Regex("^(${Regex.escape(fakeEmailAddress)})$"),
                        output = listOf(crawlableDataOf(keyUsername to fakeUsername))
                    ),

                    FakeInputProcessor(
                        tagSuffix = "brief-info-by-username",
                        inputProbingRegex = Regex("^(${Regex.escape(fakeUsername)})$"),
                        output = listOf(crawlableDataOf(keyEmailAddress to fakeEmailAddress, keyFullName to fakeFullName))
                    ),
                ),
                coroutineDispatcher = UnconfinedTestDispatcher(),
                resultStore = resultStore,
            )
        )
        session.eventListener = eventListener
        session.init()

        val crawlingFinishedEvent = eventListener.collectedEvents.find { event -> (event is Event.CrawlingFinished) }

        assertNotNull(crawlingFinishedEvent)

        val resultId = (crawlingFinishedEvent as Event.CrawlingFinished).resultId

        assertNotNull(resultStore.getById(resultId))

        val result = resultStore.getById(resultId)!!

        assertEquals(fakeFullName, result.context.getValues(keyFullName).getOrNull(0))
        assertEquals(fakeFullName, result.context.getValues(keyFullName).getOrNull(1))
        assertEquals(fakeFullName, result.context.getValues(keyFullName).getOrNull(2))
        assertEquals(fakeEmailAddress, result.context.getValues(keyEmailAddress).firstOrNull())
        assertEquals(fakeWebsite1, result.context.getValues(keyWebsite).getOrNull(0))
        assertEquals(fakeWebsite2, result.context.getValues(keyWebsite).getOrNull(1))
        assertEquals(fakePhoneNumber, result.context.getValues(keyPhoneNumber).firstOrNull())
        assertEquals(fakeUsername, result.context.getValues(keyUsername).firstOrNull())
    }

    @Test
    fun `successful exhaustive traversal - max crawling depth limit`() {
        // [ simulated web crawling; max crawl depth level = 3 ]
        //
        // root | lvl 0:
        //      > provides the initial input - url x1
        //
        // InputProcessor(url-1) | lvl 0 -> lvl 1:
        //      > receives the initial input (url x1)
        //      > produces output(url x3) | 1 output entry
        //
        // InputProcessor(url-2) x3 | lvl 1 -> lvl 2:
        //      > receives the input produced by InputProcessor(url-1)
        //      > produces output(url x3) for each input url received from the InputProcessor(url-1) | 3 output entries
        //
        // InputProcessor(url-3) x3 | lvl 2 -> lvl 3:
        //      > receives the input produced by InputProcessor(url-2)
        //      > produces output(url x3) for each input url received from the InputProcessor(url-2) | 3 output entries
        //
        // [ full stop; depth level 3 reached ]

        val resultStore = InMemoryCrawlingResultStore()
        val eventListener = TestCrawlingSessionEventListener()
        val pagedUrlBase = "www.yourwebsite.com/q?=something&page="
        val fakeUrl = "www.yourwebsite.com/q?=something"
        val maxCrawlDepth = 3

        val session = CrawlingSessionImpl(
            CrawlingSession.Config(
                initialInputs = setOf(fakeUrl),
                inputProcessingBatchSize = 10,
                maxCrawlDepth = maxCrawlDepth,
                inputProcessors = setOf(
                    FakeInputProcessor(
                        tagSuffix = "url-1",
                        inputProbingRegex = Regex("^(${Regex.escape(fakeUrl)})$"),
                        output = listOf(
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=1"),
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=2"),
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=3"),
                        )
                    ),
                    FakeInputProcessor(
                        tagSuffix = "url-2",
                        inputProbingRegex = Regex("^(${Regex.escape(pagedUrlBase)})[1-3]$"),
                        output = listOf(
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=4"),
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=5"),
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=6"),
                        )
                    ),
                    FakeInputProcessor(
                        tagSuffix = "url-3",
                        inputProbingRegex = Regex("^(${Regex.escape(pagedUrlBase)})[4-6]$"),
                        output = listOf(
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=7"),
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=8"),
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=9"),
                        )
                    ),
                    FakeInputProcessor(
                        tagSuffix = "url-4",
                        inputProbingRegex = Regex("^(${Regex.escape(pagedUrlBase)})[7-9]$"),
                        output = listOf(
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=10"),
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=11"),
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=12"),
                        )
                    ),
                    FakeInputProcessor(
                        tagSuffix = "url-5",
                        inputProbingRegex = Regex("^(${Regex.escape(pagedUrlBase)})1[0-2]$"),
                        output = listOf(
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=13"),
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=14"),
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=15"),
                        )
                    ),
                ),
                coroutineDispatcher = UnconfinedTestDispatcher(),
                resultStore = resultStore,
            )
        )
        session.eventListener = eventListener
        session.init()

        val crawlingFinishedEvent = eventListener.collectedEvents.find { event -> (event is Event.CrawlingFinished) }

        assertNotNull(crawlingFinishedEvent)

        val resultId = (crawlingFinishedEvent as Event.CrawlingFinished).resultId

        assertNotNull(resultStore.getById(resultId))

        val result = resultStore.getById(resultId)!!

        assertEquals(7, result.context.getAll().size)
    }

    @Test
    fun `successful exhaustive traversal - faulty input processor`() {
        val resultStore = InMemoryCrawlingResultStore()
        val eventListener = TestCrawlingSessionEventListener()

        val keyFullName = "full_name"
        val fakeFullName = "John Smith"
        val fakeUsername = "johny123"

        val session = CrawlingSessionImpl(
            CrawlingSession.Config(
                initialInputs = setOf(fakeUsername),
                inputProcessingBatchSize = 10,
                inputProcessors = setOf(
                    FakeInputProcessor(
                        tagSuffix = "full-name",
                        inputProbingRegex = Regex("^(${Regex.escape(fakeUsername)})$"),
                        output = listOf(crawlableDataOf(keyFullName to fakeFullName))
                    ),
                    FakeInputProcessor(
                        tagSuffix = "email-address",
                        inputProbingRegex = Regex("^(${Regex.escape(fakeFullName)})$"),
                        shouldProduceError = true,
                    ),
                ),
                coroutineDispatcher = UnconfinedTestDispatcher(),
                resultStore = resultStore,
            )
        )
        session.eventListener = eventListener
        session.init()

        val crawlingFinishedEvent = eventListener.collectedEvents.find { event -> (event is Event.CrawlingFinished) }

        assertNotNull(crawlingFinishedEvent)

        val resultId = (crawlingFinishedEvent as Event.CrawlingFinished).resultId

        assertNotNull(resultStore.getById(resultId))

        val result = resultStore.getById(resultId)!!

        assertEquals(fakeFullName, result.context.getValues(keyFullName).firstOrNull())
    }

    @Test
    fun `unsuccessful exhaustive traversal - faulty result store`() {
        val resultStore = mock<CrawlingResultStore>()
        whenever(resultStore.save(any())).then { throw IOException("Failed to save the result") }

        val eventListener = TestCrawlingSessionEventListener()
        val fakeUrl = "www.yourwebsite.com/q?=something"

        val session = CrawlingSessionImpl(
            CrawlingSession.Config(
                initialInputs = setOf(fakeUrl),
                inputProcessingBatchSize = 10,
                inputProcessors = setOf(
                    FakeInputProcessor(
                        tagSuffix = "url",
                        inputProbingRegex = Regex("^(${Regex.escape(fakeUrl)})$"),
                        output = listOf(
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=1"),
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=2"),
                            crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=3"),
                        )
                    ),
                ),
                coroutineDispatcher = UnconfinedTestDispatcher(),
                resultStore = resultStore,
            )
        )
        session.eventListener = eventListener
        session.init()

        val crawlingFinishedEvent = eventListener.collectedEvents.find { event -> (event is Event.CrawlingFailed) }

        assertNotNull(crawlingFinishedEvent)
    }

    @Test
    fun `successful exhaustive traversal - traversal with throttling`() {
        val throttler = mock<Throttler>()
        val eventListener = TestCrawlingSessionEventListener()
        val fakeUrl = "www.yourwebsite.com/q?=something"
        val inputProcessor = FakeInputProcessor(
            tagSuffix = "url",
            output = listOf(
                crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=1"),
                crawlableDataOf("url" to "www.yourwebsite.com/q?=something&page=2"),
            )
        )

        val session = CrawlingSessionImpl(
            CrawlingSession.Config(
                initialInputs = setOf(fakeUrl),
                inputProcessingBatchSize = 10,
                inputProcessors = setOf(inputProcessor),
                coroutineDispatcher = UnconfinedTestDispatcher(),
                resultStore = InMemoryCrawlingResultStore(),
                throttler = throttler,
            )
        )
        session.eventListener = eventListener
        session.init()

        val crawlingFinishedEvent = eventListener.collectedEvents.find { event -> (event is Event.CrawlingFinished) }

        assertNotNull(crawlingFinishedEvent)

        runBlocking {
            verify(throttler, times(3)).throttle(inputProcessor)
        }
    }

    @Test
    fun `successful exhaustive traversal - crawlable output`() {
        val resultStore = InMemoryCrawlingResultStore()
        val eventListener = TestCrawlingSessionEventListener()

        val keyFullName = "full_name"
        val keyProfilePicUrl = "profile_pic_url"
        val keyFilePath = "file_path"
        val keyProfileId = "profile_id"
        val fakeFullName = "John Smith"
        val fakeUsername = "johny123"
        val fakeProfileId = UniqueIds.random()
        val fakeProfilePicUrl = "https://www.example.com/images/profile_abc123.jpeg"

        val session = CrawlingSessionImpl(
            CrawlingSession.Config(
                initialInputs = setOf(fakeUsername),
                inputProcessingBatchSize = 10,
                inputProcessors = setOf(
                    FakeInputProcessor(
                        tagSuffix = "profile-info-extractor",
                        inputProbingRegex = Regex("^(${Regex.escape(fakeUsername)})$"),
                        output = listOf(
                            mapOf(
                                keyFullName to fakeFullName.asCrawlable(),
                                keyProfilePicUrl to fakeProfilePicUrl.asUncrawlable(),
                            )
                        ),
                    ),
                    FakeInputProcessor(
                        tagSuffix = "profile-id-obtainer",
                        inputProbingRegex = Regex("^(${Regex.escape(fakeFullName)})$"),
                        output = listOf(
                            crawlableDataOf(
                                keyProfileId to fakeProfileId,
                            )
                        ),
                    ),
                    FakeInputProcessor(
                        tagSuffix = "media-downloader",
                        inputProbingRegex = Regex("^(${Regex.escape(fakeProfilePicUrl)})$"),
                        output = listOf(
                            uncrawlableDataOf(
                                keyFilePath to "file:///home/user/Pictures/profile_pic_abc123.jpeg",
                            )
                        ),
                    ),
                ),
                coroutineDispatcher = UnconfinedTestDispatcher(),
                resultStore = resultStore,
            )
        )
        session.eventListener = eventListener
        session.init()

        val crawlingFinishedEvent = eventListener.collectedEvents.find { event -> (event is Event.CrawlingFinished) }

        assertNotNull(crawlingFinishedEvent)

        val resultId = (crawlingFinishedEvent as Event.CrawlingFinished).resultId

        assertNotNull(resultStore.getById(resultId))

        val result = resultStore.getById(resultId)!!

        assertEquals(fakeFullName, result.context.getValues(keyFullName).firstOrNull())
        assertEquals(fakeProfilePicUrl, result.context.getValues(keyProfilePicUrl).firstOrNull())
        assertEquals(fakeProfileId, result.context.getValues(keyProfileId).firstOrNull())
        assertTrue(result.context.getValues(keyFilePath).isEmpty())
    }

    @Test
    fun `successful exhaustive traversal - uncrawlable output`() {
        val resultStore = InMemoryCrawlingResultStore()
        val eventListener = TestCrawlingSessionEventListener()

        val keyFullName = "full_name"
        val keyProfilePicUrl = "profile_pic_url"
        val keyFilePath = "file_path"
        val fakeFullName = "John Smith"
        val fakeUsername = "johny123"
        val fakeProfilePicUrl = "https://www.example.com/images/profile_abc123.jpeg"
        val fakeFilePath = "file:///home/user/Pictures/profile_pic_abc123.jpeg"

        val session = CrawlingSessionImpl(
            CrawlingSession.Config(
                initialInputs = setOf(fakeUsername),
                inputProcessingBatchSize = 10,
                inputProcessors = setOf(
                    FakeInputProcessor(
                        tagSuffix = "profile-info-extractor",
                        inputProbingRegex = Regex("^(${Regex.escape(fakeUsername)})$"),
                        output = listOf(
                            crawlableDataOf(
                                keyFullName to fakeFullName,
                                keyProfilePicUrl to fakeProfilePicUrl,
                            )
                        ),
                    ),
                    FakeInputProcessor(
                        tagSuffix = "media-downloader",
                        inputProbingRegex = Regex("^(${Regex.escape(fakeProfilePicUrl)})$"),
                        output = listOf(
                            uncrawlableDataOf(
                                keyFilePath to fakeFilePath,
                            )
                        ),
                    ),
                ),
                coroutineDispatcher = UnconfinedTestDispatcher(),
                resultStore = resultStore,
            )
        )
        session.eventListener = eventListener
        session.init()

        val crawlingFinishedEvent = eventListener.collectedEvents.find { event -> (event is Event.CrawlingFinished) }

        assertNotNull(crawlingFinishedEvent)

        val resultId = (crawlingFinishedEvent as Event.CrawlingFinished).resultId

        assertNotNull(resultStore.getById(resultId))

        val result = resultStore.getById(resultId)!!

        assertEquals(fakeFullName, result.context.getValues(keyFullName).firstOrNull())
        assertEquals(fakeProfilePicUrl, result.context.getValues(keyProfilePicUrl).firstOrNull())
        assertEquals(fakeFilePath, result.context.getValues(keyFilePath).firstOrNull())
    }

}