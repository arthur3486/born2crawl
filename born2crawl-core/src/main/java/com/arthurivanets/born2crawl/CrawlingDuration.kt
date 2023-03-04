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
import java.time.Duration

/**
 * Holds the crawling session specific time information.
 *
 * @param startTimeMs the timestamp (Unix timestamp) of the start of the crawling session.
 * @param endTimeMs the timestamp (Unix timestamp) of the end of the crawling session.
 */
data class CrawlingDuration(
    val startTimeMs: Long,
    val endTimeMs: Long,
) : Serializable {

    /**
     * The calculated duration.
     */
    val duration: Duration

    init {
        require(endTimeMs > startTimeMs) { "End Time MUST BE greater than Start Time" }

        duration = Duration.ofMillis(endTimeMs - startTimeMs)
    }

}