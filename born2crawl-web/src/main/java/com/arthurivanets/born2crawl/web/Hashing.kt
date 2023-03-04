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

import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

internal interface HashGenerator {
    fun generate(input: String): String
}

internal class MD5HashGenerator : HashGenerator {

    override fun generate(input: String): String {
        if (input.isBlank()) {
            return input
        }

        val digest = MessageDigest.getInstance("MD5")
        val hash = digest.digest(input.toByteArray())

        return BigInteger(1, hash)
            .toString(16)
            .uppercase(Locale.US)
    }

}