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

/**
 * Holds the information about the result of the processing of a particular input.
 *
 * @param source the origin of the output (i.e. an identifier of a particular [InputProcessor]).
 * @param startedBy an identifier of an [InputProcessor] that produced the output that was subsequently used as an input for this processing.
 * @param input the input that was used to initiate the processing.
 * @param data the result of the processing of the input.
 * @param timestamp the timestamp (Unix timestamp) of the creation of the output.
 */
data class CrawlingOutput(
    val source: Source,
    val startedBy: Source,
    val input: String,
    val data: List<Map<String, String>>,
    val timestamp: Long,
) : Serializable