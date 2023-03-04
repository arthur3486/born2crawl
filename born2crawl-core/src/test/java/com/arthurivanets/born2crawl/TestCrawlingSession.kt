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

class TestCrawlingSession(private val config: CrawlingSession.Config) : CrawlingSession {

    override val sessionId: String = UniqueIds.random()
    override var eventListener: ((CrawlingSession.Event) -> Unit)? = null

    var state: State = State.INITIAL
        private set

    enum class State {
        INITIAL,
        INITIALIZED,
        DESTROYED,
    }

    override fun init() {
        state = State.INITIALIZED
    }

    override fun destroy() {
        state = State.DESTROYED
    }

    fun signalCrawlingFinished() {
        eventListener?.invoke(
            CrawlingSession.Event.CrawlingFinished(
                resultId = UniqueIds.random(),
                sessionId = sessionId,
                initialInputs = config.initialInputs,
                crawlingDuration = crawlingDuration(),
            )
        )
    }

    fun signalCrawlingFailed() {
        eventListener?.invoke(
            CrawlingSession.Event.CrawlingFailed(
                error = RuntimeException("Something went wrong"),
                sessionId = sessionId,
                initialInputs = config.initialInputs,
                crawlingDuration = crawlingDuration(),
            )
        )
    }

    private fun crawlingDuration(): CrawlingDuration {
        return CrawlingDuration(System.currentTimeMillis(), (System.currentTimeMillis() + 100L))
    }

}