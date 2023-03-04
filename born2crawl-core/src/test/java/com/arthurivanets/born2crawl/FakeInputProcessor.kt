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

class FakeInputProcessor(
    tagSuffix: String = UniqueIds.random(),
    private val inputProbingRegex: Regex = Regex(".+"),
    private val output: List<Map<String, InputProcessor.ValueHolder>> = emptyList(),
    private val shouldProduceError: Boolean = false,
) : InputProcessor {

    override val source = Source("FakeProcessor-$tagSuffix", UniqueIds.random())

    override fun canProcess(input: CrawlingInput, context: CrawlingContext): Boolean {
        return input.rawInput.matches(inputProbingRegex)
    }

    override fun process(input: CrawlingInput, context: CrawlingContext): InputProcessor.Result {
        return if (shouldProduceError) {
            InputProcessor.Result.Failure(RuntimeException("Something went wrong"))
        } else {
            InputProcessor.Result.Success(
                InputProcessor.Output(
                    source = source,
                    startedBy = input.source,
                    input = input.rawInput,
                    data = output,
                    timestamp = System.currentTimeMillis(),
                )
            )
        }
    }

}