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

/**
 * A contract for the implementation of concrete result stores.
 *
 * [CrawlingResultStore] is used by the [CrawlingSession]s to store the crawling results.
 */
interface CrawlingResultStore {

    /**
     * Saves the specified [CrawlingResult] in this store.
     *
     * @param result the result to be saved.
     * @return the unique id of the saved result
     */
    fun save(result: CrawlingResult): String

    /**
     * Retrieves the [CrawlingResult] from the store that matches the specified id (if any).
     *
     * @param id a unique id of the result.
     * @return a matching [CrawlingResult] or null if no matching results were found
     */
    fun getById(id: String): CrawlingResult?

    /**
     * Retrieves all stored [CrawlingResult]s.
     *
     * @return a list of available [CrawlingResult]s
     */
    fun getAll(): List<CrawlingResult>

    /**
     * Deletes the stored [CrawlingResult] that matches the specified id (if any).
     *
     * @param id a unique id of the result.
     */
    fun deleteById(id: String)

    /**
     * Deletes all stored [CrawlingResult]s.
     */
    fun deleteAll()

}