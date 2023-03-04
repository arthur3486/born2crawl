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

package com.arthurivanets.born2crawl.web

import com.arthurivanets.born2crawl.CrawlingContext
import com.arthurivanets.born2crawl.CrawlingInput
import com.arthurivanets.born2crawl.InputProcessor
import com.arthurivanets.born2crawl.Source
import com.arthurivanets.born2crawl.util.UniqueIds
import com.arthurivanets.born2crawl.util.logger
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * A contract for the page processing delegate.
 */
typealias PageProcessor = ((Document, CrawlingInput, CrawlingContext) -> List<Map<String, InputProcessor.ValueHolder>>)

/**
 * An implementation of the [InputProcessor] that allows to crawl the web pages.
 *
 * @param pageProcessor processor of the content of the downloaded web pages.
 * @param inputProber input filter (accepts all urls by default).
 * @param userAgentProvider provider of values for the user agent http header of the web page download requests.
 * @param extraHeadersProvider provider of the additional http headers for the web page download requests.
 * @param proxyConfig proxy server configuration (optional).
 */
class WebPageCrawler(
    private val pageProcessor: PageProcessor,
    private val inputProber: InputProber = InputProbers.url(),
    private val userAgentProvider: UserAgentProvider = { "WebPageCrawler" },
    private val extraHeadersProvider: HeadersProvider = { emptyMap() },
    private val proxyConfig: ProxyConfig? = null,
) : InputProcessor {

    override val source = Source("web-crawler", UniqueIds.random())

    private val logger = logger(source.id)

    override fun canProcess(input: CrawlingInput, context: CrawlingContext): Boolean {
        return inputProber(input, context)
    }

    override fun process(input: CrawlingInput, context: CrawlingContext): InputProcessor.Result {
        return try {
            logger.info("Processing: $input")

            val document = Jsoup.connect(input.rawInput)
                .headers(extraHeadersProvider())
                .userAgent(userAgentProvider())
                .applyProxyConfig()
                .followRedirects(true)
                .get()

            val result = pageProcessor(document, input, context)
            val output = InputProcessor.Output(
                source = source,
                startedBy = input.source,
                input = input.rawInput,
                data = result,
                timestamp = System.currentTimeMillis(),
            )

            logger.info("Successfully processed: $input")

            InputProcessor.Result.Success(output)
        } catch (error: Throwable) {
            logger.error("Failed to process: $input", error)

            InputProcessor.Result.Failure(error)
        }
    }

    private fun Connection.applyProxyConfig(): Connection {
        return if (proxyConfig != null) {
            proxy(proxyConfig.host, proxyConfig.port)
        } else {
            this
        }
    }

}