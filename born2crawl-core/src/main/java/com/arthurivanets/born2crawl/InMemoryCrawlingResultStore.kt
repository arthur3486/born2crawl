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

import com.arthurivanets.born2crawl.util.UniqueIds
import java.util.concurrent.ConcurrentHashMap

/**
 * A concrete implementation of the [CrawlingResultStore] that stores the crawling results in the main memory of the computer.
 */
class InMemoryCrawlingResultStore : CrawlingResultStore {

    private val resultsById = ConcurrentHashMap<String, CrawlingResult>()

    override fun save(result: CrawlingResult): String {
        val resultId = UniqueIds.random()
        resultsById[resultId] = result

        return resultId
    }

    override fun getById(id: String): CrawlingResult? {
        return resultsById[id]
    }

    override fun getAll(): List<CrawlingResult> {
        return resultsById.values.toList()
    }

    override fun deleteById(id: String) {
        resultsById.remove(id)
    }

    override fun deleteAll() {
        resultsById.clear()
    }

}