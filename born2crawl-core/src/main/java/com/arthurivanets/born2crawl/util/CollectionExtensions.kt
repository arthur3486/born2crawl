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

package com.arthurivanets.born2crawl.util

import com.arthurivanets.born2crawl.CrawlFrontier

internal fun <T> CrawlFrontier<T>.remove(numberOfItems: Int): List<T> {
    require(numberOfItems >= 0) { "Number of items to be removed cannot be lower than 0. Requested number of items: $numberOfItems" }

    val removedItems = mutableListOf<T>()

    while (!this.isEmpty() && (removedItems.size < numberOfItems)) {
        this.remove()?.let(removedItems::add)
    }

    return removedItems
}
