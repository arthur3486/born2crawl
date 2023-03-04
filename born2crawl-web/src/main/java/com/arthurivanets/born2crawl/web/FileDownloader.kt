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
import com.arthurivanets.born2crawl.InputProcessor.Output.Companion.uncrawlableDataOf
import com.arthurivanets.born2crawl.Source
import com.arthurivanets.born2crawl.util.UniqueIds
import com.arthurivanets.born2crawl.util.logger
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.time.Duration

/**
 * An implementation of the [InputProcessor] that allows to download files by urls.
 *
 * @param outputDir directory into which the files will get downloaded.
 * @param inputProber input filter (accepts all urls by default).
 * @param userAgentProvider provider of values for the user agent http header of the file download requests.
 * @param allowedContentTypes a set of file MIME types that can be downloaded.
 * @param extraHeadersProvider provider of the additional http headers for the file download requests.
 * @param proxyConfig proxy server configuration (optional).
 */
class FileDownloader(
    private val outputDir: File,
    private val inputProber: InputProber = InputProbers.url(),
    private val userAgentProvider: UserAgentProvider = { "FileDownloader" },
    private val allowedContentTypes: Set<MimeType> = MimeTypes.values().toSet(),
    private val extraHeadersProvider: HeadersProvider = { emptyMap() },
    private val proxyConfig: ProxyConfig? = null,
) : InputProcessor {

    override val source = Source("file-downloader", UniqueIds.random())

    private val logger = logger(source.id)
    private val hashGenerator = MD5HashGenerator()

    private val httpClient = OkHttpClient.Builder()
        .applyProxyConfig()
        .followRedirects(true)
        .connectTimeout(Duration.ofSeconds(30))
        .readTimeout(Duration.ofSeconds(30))
        .writeTimeout(Duration.ofSeconds(30))
        .build()

    init {
        require(outputDir.isDirectory) { "Please, provide a valid output directory. Provided: $outputDir" }
        require(outputDir.isWritable()) { "Please, ensure that the output directory has write permissions." }
    }

    override fun canProcess(input: CrawlingInput, context: CrawlingContext): Boolean {
        if (!inputProber(input, context)) {
            return false
        }

        return try {
            getContentType(input.rawInput)
                .let(::isAllowedContentType)
        } catch (error: Throwable) {
            false
        }
    }

    override fun process(input: CrawlingInput, context: CrawlingContext): InputProcessor.Result {
        try {
            logger.info("Downloading the file: $input")

            val result = downloadFile(input.rawInput)
            val output = InputProcessor.Output(
                source = source,
                startedBy = input.source,
                input = input.rawInput,
                data = listOf(result),
                timestamp = System.currentTimeMillis(),
            )

            logger.info("Successfully downloaded the file: $input")

            return InputProcessor.Result.Success(output)
        } catch (error: Throwable) {
            logger.error("Failed to download the file: $input", error)

            return InputProcessor.Result.Failure(error)
        }
    }

    private fun getContentType(url: String): String? {
        return Request.Builder()
            .url(url)
            .head()
            .addUserAgent()
            .addHeaders(extraHeadersProvider())
            .build()
            .let(httpClient::newCall)
            .execute()
            .use { response -> response.headers[HttpHeaders.CONTENT_TYPE] }
    }

    private fun downloadFile(url: String): Map<String, InputProcessor.ValueHolder> {
        return Request.Builder()
            .url(url)
            .get()
            .addUserAgent()
            .addHeaders(extraHeadersProvider())
            .build()
            .let(httpClient::newCall)
            .execute()
            .use { response ->
                val contentType = checkNotNull(response.headers[HttpHeaders.CONTENT_TYPE]) {
                    "No valid Content-Type header provided by $url"
                }
                val body = checkNotNull(response.body) { "No valid response body provided by $url" }
                val file = createFile(url, contentType)

                file.outputStream().use { outputStream ->
                    body.byteStream().copyTo(outputStream)
                }

                uncrawlableDataOf(
                    "file_path" to file.absolutePath,
                    "file_uri" to file.toURI().toString(),
                )
            }
    }

    private fun createFile(url: String, contentType: String): File {
        fun normalize(rawValue: String): String {
            return rawValue.replace(Regex("[^a-zA-Z_0-9@]"), "_")
        }

        val uri = URI.create(url)
        val lastPathSegment = (uri.path.split("/").lastOrNull()?.toString() ?: "")
        val urlHash = hashGenerator.generate(url)
        val fileExtension = (MimeTypes.guessFileExtension(contentType)?.let { ".$it" } ?: "")
        val fileName = (normalize(lastPathSegment) + "_" + urlHash + fileExtension)
        val finalDir = File(outputDir, normalize(uri.host))

        finalDir.mkdirs()

        return File(finalDir, fileName)
    }

    private fun Request.Builder.addUserAgent(): Request.Builder {
        return addHeader(HttpHeaders.USER_AGENT, userAgentProvider())
    }

    private fun Request.Builder.addHeaders(headers: Map<String, String>) = apply {
        headers.forEach { (key, value) -> addHeader(key, value) }
    }

    private fun OkHttpClient.Builder.applyProxyConfig(): OkHttpClient.Builder {
        return if (proxyConfig != null) {
            proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyConfig.host, proxyConfig.port)))
        } else {
            this
        }
    }

    private fun isAllowedContentType(type: String?): Boolean {
        if (type == null) {
            return false
        }

        return allowedContentTypes.find { allowedType -> allowedType.matches(type) } != null
    }

    private fun File.isWritable(): Boolean {
        return try {
            canWrite()
        } catch (e: SecurityException) {
            false
        }
    }

}
