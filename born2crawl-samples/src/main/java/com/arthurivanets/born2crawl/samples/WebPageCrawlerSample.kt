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

package com.arthurivanets.born2crawl.samples

import com.arthurivanets.born2crawl.*
import com.arthurivanets.born2crawl.InputProcessor.Output.Companion.crawlableDataOf
import com.arthurivanets.born2crawl.ThrottlingConfig.PerProcessor.Companion.delay
import com.arthurivanets.born2crawl.stores.FileCrawlingResultStore
import com.arthurivanets.born2crawl.web.PageProcessor
import com.arthurivanets.born2crawl.web.WebPageCrawler
import org.jsoup.nodes.Document
import java.io.File
import java.time.Duration

private object WikiPageProcessor : PageProcessor {

    override fun invoke(page: Document, input: CrawlingInput, context: CrawlingContext): List<Map<String, InputProcessor.ValueHolder>> {
        return page.select("a.mw-redirect")
            .map { element -> ("https://en.wikipedia.org" + element.attr("href")) }
            .toSet()
            .map { url -> crawlableDataOf("url" to url) }
            .take(5)
    }

}

fun main() {
    val resultsOutputDir = requireNotNull(System.getenv("CRAWLER_DEMO_RESULT_OUTPUT_DIR")?.let(::File)) {
        "Please, specify a valid result output directory via the ENV variable: CRAWLER_DEMO_RESULT_OUTPUT_DIR"
    }
    val resultStore = FileCrawlingResultStore(resultsOutputDir)

    val crawler = Crawler(
        Crawler.Config(
            inputProcessors = setOf(
                WebPageCrawler(pageProcessor = WikiPageProcessor),
            ),
            resultStore = resultStore,
            throttlingConfig = ThrottlingConfig.PerProcessor(
                delay<WebPageCrawler>(Duration.ofMillis(250)),
            ),
            maxCrawlDepth = 2,
            eventListener = { event -> println("CrawlingEvent: $event") },
        )
    )

    crawler.submit("https://en.wikipedia.org/wiki/Microprocessor", "https://en.wikipedia.org/wiki/Texas_Instruments_TMS1000")

    Thread.currentThread().join()
}
