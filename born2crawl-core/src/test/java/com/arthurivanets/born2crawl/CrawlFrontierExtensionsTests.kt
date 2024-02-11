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

import com.arthurivanets.born2crawl.util.remove
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class CrawlFrontierExtensionsTests {

    @Test
    fun `remove multiple items - invalid number of items`() {
        val frontier = TraversalAlgorithm.DFS.createCrawlFrontier<Int>()

        assertThrows<IllegalArgumentException> {
            frontier.remove(-1)
        }
    }

    @Test
    fun `remove multiple items - number of items is less than total size`() {
        val frontier = TraversalAlgorithm.DFS.createCrawlFrontier<Int>()
        frontier.add(1)
        frontier.add(2)
        frontier.add(3)
        frontier.add(4)

        val items = frontier.remove(2)

        assertEquals(2, items.size)
        assertEquals(4, items[0])
        assertEquals(3, items[1])
    }

    @Test
    fun `remove multiple items - number of items is greater than total size`() {
        val frontier = TraversalAlgorithm.DFS.createCrawlFrontier<Int>()
        frontier.add(1)
        frontier.add(2)
        frontier.add(3)

        val items = frontier.remove(4)

        assertEquals(3, items.size)
        assertEquals(3, items[0])
        assertEquals(2, items[1])
        assertEquals(1, items[2])
    }

}
