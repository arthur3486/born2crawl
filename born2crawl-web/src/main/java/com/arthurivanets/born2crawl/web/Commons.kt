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

package com.arthurivanets.born2crawl.web

import com.arthurivanets.born2crawl.CrawlingContext
import com.arthurivanets.born2crawl.CrawlingInput

/**
 * Provides the value for the User-Agent header of the crawling requests.
 */
typealias UserAgentProvider = (() -> String)

/**
 * Checks if the specified [CrawlingInput] can be processed.
 *
 * NOTE: it MUST NEVER throw exceptions.
 */
typealias InputProber = ((CrawlingInput, CrawlingContext) -> Boolean)

/**
 * Provides additional HTTP headers for the crawling requests.
 */
typealias HeadersProvider = (() -> Map<String, String>)

/**
 * A set of common [InputProber] factories.
 */
object InputProbers {

    /**
     * Creates an [InputProber] that matches all urls.
     */
    @JvmStatic
    fun url(): InputProber {
        val urlRegex =
            Regex("^(?:(?:http|https)://)(?:\\S+(?::\\S*)?@)?(?:(?:(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[0-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\u00a1-\\uffff0-9]+-?)*[a-z\\u00a1-\\uffff0-9]+)(?:\\.(?:[a-z\\u00a1-\\uffff0-9]+-?)*[a-z\\u00a1-\\uffff0-9]+)*(?:\\.(?:[a-z\\u00a1-\\uffff]{2,})))|localhost)(?::\\d{2,5})?(?:(/|\\?|#)[^\\s]*)?$")
        return { input, _ -> input.rawInput.matches(urlRegex) }
    }

}