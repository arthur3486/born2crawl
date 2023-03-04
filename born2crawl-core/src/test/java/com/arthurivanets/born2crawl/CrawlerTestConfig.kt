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

import kotlinx.coroutines.CoroutineDispatcher

fun Crawler.Companion.TestConfig(
    inputProcessors: Set<InputProcessor>,
    resultStore: CrawlingResultStore,
    eventListener: ((Crawler.Event) -> Unit),
    crawlingSessionFactory: ((CrawlingSession.Config) -> CrawlingSession),
    coroutineDispatcher: CoroutineDispatcher,
    throttlingConfig: ThrottlingConfig = ThrottlingConfig.NoOp,
    crawlingSessionParallelism: Int = 10,
    inputProcessingBatchSize: Int = 10,
    maxCrawlDepth: Int = Int.MAX_VALUE,
): Crawler.Config {
    return object : Crawler.Config {
        override val inputProcessors = inputProcessors
        override val resultStore = resultStore
        override val crawlingSessionParallelism = crawlingSessionParallelism
        override val inputProcessingBatchSize = inputProcessingBatchSize
        override val maxCrawlDepth = maxCrawlDepth
        override val coroutineDispatcher = coroutineDispatcher
        override val throttlingConfig = throttlingConfig
        override val crawlingSessionFactory = crawlingSessionFactory
        override val eventListener = eventListener
    }
}