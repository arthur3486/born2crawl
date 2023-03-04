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

/**
 * A contract for the implementation of concrete MIME types.
 */
interface MimeType {

    /**
     * Raw MIME type (e.g. image/jpeg).
     */
    val value: String

    /**
     * Checks if the current MIME type matches the specified [type].
     *
     * (e.g. if the current MIME type is "image/`*`" it would be logical to accept the following [type]s: "image/jpeg", "image/png", etc.)
     */
    fun matches(type: String): Boolean

}

/**
 * A set of common file MIME types.
 */
enum class MimeTypes(override val value: String) : MimeType {

    IMAGE_ALL("image/*"),
    VIDEO_ALL("video/*"),
    AUDIO_ALL("audio/*"),
    TEXT_ALL("text/*");

    companion object {

        private val MIME_TYPE_EXTENSIONS = mapOf(
            // images
            "image/apng" to "apng",
            "image/avif" to "avif",
            "image/gif" to "gif",
            "image/jpeg" to "jpeg",
            "image/gif" to "gif",
            "image/jpeg" to "jpeg",
            "image/png" to "png",
            "image/svg+xml" to "svg",
            "image/webp" to "webp",

            // video
            "video/x-msvideo" to "avi",
            "video/mp4" to "mp4",
            "video/mpeg" to "mpeg",
            "video/ogg" to "ogv",
            "video/webm" to "webm",
            "video/3gpp" to "3gp",

            // audio
            "audio/aac" to "aac",
            "audio/midi" to "midi",
            "audio/x-midi" to "midi",
            "audio/mpeg" to "mp3",
            "audio/ogg" to "oga",
            "audio/opus" to "opus",
            "audio/wav" to "wav",
            "audio/webm" to "weba",
        )

        @JvmStatic
        internal fun guessFileExtension(rawMimeType: String): String? {
            val matchedExtension = MIME_TYPE_EXTENSIONS[rawMimeType]

            return when {
                (matchedExtension != null) -> matchedExtension
                TEXT_ALL.matches(rawMimeType) -> "txt"
                else -> null
            }
        }

    }

    override fun matches(type: String): Boolean {
        val category = this.value.split("/").first()
        return type.startsWith("$category/", ignoreCase = true)
    }

}