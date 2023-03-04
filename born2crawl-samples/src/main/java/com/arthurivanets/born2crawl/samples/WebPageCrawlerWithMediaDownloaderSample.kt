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
import com.arthurivanets.born2crawl.web.FileDownloader
import com.arthurivanets.born2crawl.web.MimeTypes
import com.arthurivanets.born2crawl.web.PageProcessor
import com.arthurivanets.born2crawl.web.WebPageCrawler
import org.jsoup.nodes.Document
import java.io.File
import java.time.Duration

private object UnsplashPageProcessor : PageProcessor {

    override fun invoke(page: Document, input: CrawlingInput, context: CrawlingContext): List<Map<String, InputProcessor.ValueHolder>> {
        return page.select("img.a5VGX")
            .map { element -> element.attr("src") }
            .toSet()
            .map { url -> crawlableDataOf("url" to url) }
    }

}

fun main() {
    val resultsOutputDir = requireNotNull(System.getenv("CRAWLER_DEMO_RESULT_OUTPUT_DIR")?.let(::File)) {
        "Please, specify a valid result output directory via the ENV variable: CRAWLER_DEMO_RESULT_OUTPUT_DIR"
    }
    val mediaFilesOutputDir = File(resultsOutputDir, "media").also { file -> file.mkdirs() }
    val resultStore = FileCrawlingResultStore(resultsOutputDir)

    val crawler = Crawler(
        Crawler.Config(
            inputProcessors = setOf(
                WebPageCrawler(
                    inputProber = { input, _ -> input.rawInput.matches(Regex("^${Regex.escape("https://unsplash.com/")}.*$")) },
                    pageProcessor = UnsplashPageProcessor,
                ),
                FileDownloader(
                    outputDir = mediaFilesOutputDir,
                    allowedContentTypes = setOf(MimeTypes.IMAGE_ALL, MimeTypes.VIDEO_ALL),
                ),
            ),
            resultStore = resultStore,
            throttlingConfig = ThrottlingConfig.PerProcessor(
                delay<WebPageCrawler>(Duration.ofSeconds(1)),
                delay<FileDownloader>(Duration.ofMillis(250L)),
            ),
            eventListener = { event -> println("CrawlingEvent: $event") },
        )
    )

    crawler.submit("https://unsplash.com/t/architecture-interior")

    Thread.currentThread().join()
}