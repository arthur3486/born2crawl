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

import com.arthurivanets.born2crawl.util.pop
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

internal class LinkedListExtensionsTests {

    @Test
    fun `pop multiple items - invalid number of items`() {
        val stack = LinkedList<Int>()

        assertThrows<IllegalArgumentException> {
            stack.pop(-1)
        }
    }

    @Test
    fun `pop multiple items - number of items is less than total size`() {
        val stack = LinkedList<Int>()
        stack.push(1)
        stack.push(2)
        stack.push(3)
        stack.push(4)

        val items = stack.pop(2)

        assertEquals(2, items.size)
        assertEquals(4, items[0])
        assertEquals(3, items[1])
    }

    @Test
    fun `pop multiple items - number of items is greater than total size`() {
        val stack = LinkedList<Int>()
        stack.push(1)
        stack.push(2)
        stack.push(3)

        val items = stack.pop(4)

        assertEquals(3, items.size)
        assertEquals(3, items[0])
        assertEquals(2, items[1])
        assertEquals(1, items[2])
    }

}