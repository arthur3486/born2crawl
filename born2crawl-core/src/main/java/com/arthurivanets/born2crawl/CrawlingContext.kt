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

import java.io.Serializable
import java.util.*

/**
 * Returns an empty new [MutableCrawlingContext].
 */
fun MutableCrawlingContext(): MutableCrawlingContext {
    return MutableCrawlingContextImpl()
}

/**
 * A set of qualifiers used for the retrieval of the [CrawlingOutput]s from the [CrawlingContext].
 */
sealed interface Qualifier : Serializable {
    data class SourceName(val name: String) : Qualifier
    data class SourceId(val id: String) : Qualifier
}

/**
 * A read-only variant of the crawling context.
 *
 * Crawling context is a data structure that is intended to hold the results of a single [CrawlingSession].
 */
interface CrawlingContext : Serializable {

    /**
     * Retrieves the [CrawlingOutput]s that match the specified [qualifier].
     *
     * @param qualifier result filter
     * @return a list of [CrawlingOutput]s
     */
    fun get(qualifier: Qualifier): List<CrawlingOutput>

    /**
     * Retrieves all [CrawlingOutput] property values that match the specified property [key].
     *
     * @param key property key filter
     * @return a list of property values
     */
    fun getValues(key: String): List<String>

    /**
     * Retrieves all available [CrawlingOutput]s.
     *
     * @return a list of [CrawlingOutput]s
     */
    fun getAll(): List<CrawlingOutput>

}

/**
 * A read+write variant of the crawling context.
 *
 * Crawling context is a data structure that is intended to hold the results of a single [CrawlingSession].
 */
interface MutableCrawlingContext : CrawlingContext {

    /**
     * Pushes the specified [CrawlingOutput] into the current context.
     *
     * @param output the crawling output
     */
    fun push(output: CrawlingOutput)

    /**
     * Pushes the specified [CrawlingOutput]s into the current context.
     *
     * @param outputs the crawling outputs
     */
    fun push(outputs: Iterable<CrawlingOutput>)

    /**
     * Creates a read-only view of this [MutableCrawlingContext].
     *
     * @return a read-only view of the current context
     */
    fun readOnly(): CrawlingContext

}

private class ReadOnlyCrawlingContext(private val originalContext: CrawlingContext) : CrawlingContext by originalContext

private class MutableCrawlingContextImpl : MutableCrawlingContext {

    private val outputStore = LinkedList<CrawlingOutput>()

    override fun get(qualifier: Qualifier): List<CrawlingOutput> {
        return when (qualifier) {
            is Qualifier.SourceId -> outputStore.filter { output -> (output.source.id == qualifier.id) }
            is Qualifier.SourceName -> outputStore.filter { output -> (output.source.name == qualifier.name) }
        }
    }

    override fun getValues(key: String): List<String> {
        return outputStore.map { output ->
            output.data.map { dataPiece -> dataPiece[key] }
        }
            .flatten()
            .filterNotNull()
    }

    override fun getAll(): List<CrawlingOutput> {
        return outputStore
    }

    override fun push(output: CrawlingOutput) {
        outputStore.push(output)
    }

    override fun push(outputs: Iterable<CrawlingOutput>) {
        outputs.forEach(outputStore::push)
    }

    override fun readOnly(): CrawlingContext {
        return ReadOnlyCrawlingContext(this)
    }

}