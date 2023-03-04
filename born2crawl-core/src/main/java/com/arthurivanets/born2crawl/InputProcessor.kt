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

import com.arthurivanets.born2crawl.InputProcessor.ValueHolder.Companion.asCrawlable
import com.arthurivanets.born2crawl.InputProcessor.ValueHolder.Companion.asUncrawlable

/**
 * A contract for the implementation of concrete input processors.
 *
 * [InputProcessor]s play a crucial role in crawling, as these are the entities that transform the initial input data
 * into more pieces of crawlable data (provide the "food" for the crawling engine, so to say).
 * Without [InputProcessor]s crawling engine will simply not be able to work.
 *
 * The [InputProcessor] contract does not impose any restrictions on the types of data sources that can be used for crawling.
 * So, you can freely implement an [InputProcessor] for basically any data source that comes to your mind, including but not limited to
 * file systems, web pages, remote apis, databases, etc.
 */
interface InputProcessor {

    data class ValueHolder internal constructor(
        val value: String,
        val isCrawlable: Boolean,
    ) {

        companion object {

            /**
             * Makes the raw output value crawlable (i.e. allows the crawling session to process the value).
             */
            @JvmStatic
            fun String.asCrawlable(): ValueHolder = ValueHolder(this, true)

            /**
             * Makes the raw output value uncrawlable (i.e. forbids further processing of the value by the crawling session).
             */
            @JvmStatic
            fun String.asUncrawlable(): ValueHolder = ValueHolder(this, false)

        }

    }

    /**
     * Holds the results of a successful input processing.
     *
     * @param source the origin of the output (i.e. an identifier of a particular [InputProcessor]).
     * @param startedBy an identifier of an [InputProcessor] that produced the output that was subsequently used as an input for this processing.
     * @param input the input that was used to initiate the processing.
     * @param data the result of the processing of the input.
     * @param timestamp the timestamp (Unix timestamp) of the creation of the output.
     */
    data class Output(
        val source: Source,
        val startedBy: Source,
        val input: String,
        val data: List<Map<String, ValueHolder>>,
        val timestamp: Long,
    ) {

        companion object {

            /**
             * Creates an output data part whose values are crawable.
             * (Crawlable values can be reprocessed by the crawling session)
             */
            @JvmStatic
            fun crawlableDataOf(vararg properties: Pair<String, String>): Map<String, ValueHolder> {
                return properties.associate { (key, value) -> key to value.asCrawlable() }
            }

            /**
             * Creates an output data part whose values are uncrawable.
             * (Uncrawlable values cannot be reprocessed by the crawling session)
             */
            @JvmStatic
            fun uncrawlableDataOf(vararg properties: Pair<String, String>): Map<String, ValueHolder> {
                return properties.associate { (key, value) -> key to value.asUncrawlable() }
            }

        }

    }

    /**
     * Input processing result variants.
     */
    sealed interface Result {
        data class Success(val output: Output) : Result
        data class Failure(val error: Throwable) : Result
    }

    /**
     * An identifier of this input processor.
     *
     * NOTE: it's recommended to create a unique [Source] for each instance of the [InputProcessor].
     */
    val source: Source

    /**
     * Checks if this [InputProcessor] can process the specified [input].
     *
     * This method gets called on a background thread, so your input checks can safely include
     * file reads, networks calls, and/or any other time-consuming operations.
     *
     * NOTE: this method MUST NEVER throw exceptions.
     *
     * @param input the input to be probed.
     * @param context the snapshot of the data that's been crawled by the session so far.
     * @return true if this input process can process the specified input, false otherwise
     */
    fun canProcess(input: CrawlingInput, context: CrawlingContext): Boolean

    /**
     * Processes the specified [input].
     *
     * NOTE: this method MUST NEVER throw exceptions.
     *
     * @param input the input to be probed.
     * @param context the snapshot of the data that's been crawled by the session so far.
     * @return the result of the processing of the specified input
     */
    fun process(input: CrawlingInput, context: CrawlingContext): Result

}