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

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Obtains a logger instance that's named according to the class of the object upon which this method is called.
 *
 * @param nameSuffix an optional suffix that will be added to the name of the logger
 * @return [Logger] instance
 */
inline fun <reified T : Any> T.logger(nameSuffix: String? = null): Logger {
    val suffix = (nameSuffix?.let { "-$it" } ?: "")
    return LoggerFactory.getLogger(T::class.java.name + suffix)
}