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

import java.util.*

/**
 * A set of utilities for the generation of unique ids.
 */
object UniqueIds {

    /**
     * Generates a random unique id comprised of alphanumeric characters.
     *
     * @return generated id
     */
    @JvmStatic
    fun random(): String {
        return UUID.randomUUID()
            .toString()
            .replace("[^a-zA-Z0-9]", "")
            .uppercase(Locale.US)
    }

}