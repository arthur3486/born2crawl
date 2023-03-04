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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class CrawlingContextTests {

    @Test
    fun `info retrieval by source id`() {
        val source1 = Source("web-1", UniqueIds.random())
        val source2 = Source("web-2", UniqueIds.random())

        val context = MutableCrawlingContext()

        context.push(fakeOutput(source1, listOf(mapOf("url" to "https://something.domain/s?p=1"))))
        context.push(fakeOutput(source1, listOf(mapOf("url" to "https://something.domain/s?p=2"))))
        context.push(fakeOutput(source2, listOf(mapOf("url" to "https://somethingelse.domain"))))

        val source1Output = context.get(Qualifier.SourceId(source1.id)).simplify()

        assertEquals(2, source1Output.size)
        assertNotNull(source1Output.find { data -> data["url"] == "https://something.domain/s?p=1" })
        assertNotNull(source1Output.find { data -> data["url"] == "https://something.domain/s?p=2" })

        val source2Output = context.get(Qualifier.SourceId(source2.id)).simplify()

        assertEquals(1, source2Output.size)
        assertNotNull(source2Output.find { data -> data["url"] == "https://somethingelse.domain" })
    }

    @Test
    fun `info retrieval by source name`() {
        val source1 = Source("web-1", UniqueIds.random())
        val source2 = Source("web-2", UniqueIds.random())

        val context = MutableCrawlingContext()

        context.push(fakeOutput(source1, listOf(mapOf("url" to "https://something.domain/s?p=1"))))
        context.push(fakeOutput(source1, listOf(mapOf("url" to "https://something.domain/s?p=2"))))
        context.push(fakeOutput(source2, listOf(mapOf("url" to "https://somethingelse.domain"))))

        val source1Output = context.get(Qualifier.SourceName(source1.name)).simplify()

        assertEquals(2, source1Output.size)
        assertNotNull(source1Output.find { data -> data["url"] == "https://something.domain/s?p=1" })
        assertNotNull(source1Output.find { data -> data["url"] == "https://something.domain/s?p=2" })

        val source2Output = context.get(Qualifier.SourceName(source2.name)).simplify()

        assertEquals(1, source2Output.size)
        assertNotNull(source2Output.find { data -> data["url"] == "https://somethingelse.domain" })
    }

    @Test
    fun `value retrieval by property key`() {
        val source1 = Source("web-1", UniqueIds.random())
        val source2 = Source("web-2", UniqueIds.random())

        val context = MutableCrawlingContext()

        context.push(fakeOutput(source1, listOf(mapOf("url" to "https://something.domain/s?p=1"))))
        context.push(fakeOutput(source1, listOf(mapOf("url" to "https://something.domain/s?p=2"))))
        context.push(fakeOutput(source2, listOf(mapOf("url" to "https://somethingelse.domain"))))

        val values = context.getValues("url").toSet()

        assertEquals(3, values.size)
        assertTrue("https://something.domain/s?p=1" in values)
        assertTrue("https://something.domain/s?p=2" in values)
        assertTrue("https://somethingelse.domain" in values)
    }

    private fun fakeOutput(source: Source, data: List<Map<String, String>>): CrawlingOutput {
        return CrawlingOutput(
            source = source,
            startedBy = Source("fake-source", UniqueIds.random()),
            input = "fake input",
            data = data,
            timestamp = System.currentTimeMillis(),
        )
    }

    private fun List<CrawlingOutput>.simplify(): List<Map<String, String>> {
        return this.map { output -> output.data }
            .flatten()
    }

}