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

import com.arthurivanets.born2crawl.InputProcessor.Result
import com.arthurivanets.born2crawl.util.UniqueIds
import com.arthurivanets.born2crawl.util.logger
import com.arthurivanets.born2crawl.util.remove
import kotlinx.coroutines.*

internal class CrawlingSessionImpl(private val config: CrawlingSession.Config) : CrawlingSession {

    override val sessionId = UniqueIds.random()

    private val log = logger(sessionId)

    private val uncaughtExceptionHandler = CoroutineExceptionHandler { ctx, error ->
        log.error("Uncaught exception", error)
    }

    private val sessionScope = CoroutineScope(config.coroutineDispatcher + SupervisorJob() + uncaughtExceptionHandler)
    private val processedInputsTracker = mutableSetOf<String>()

    override var eventListener: ((CrawlingSession.Event) -> Unit)? = null

    private class TraversalData<T : Any>(
        val crawlDepth: Int,
        val value: T
    )

    override fun init() {
        sessionScope.launch {
            val startTime = System.currentTimeMillis()

            log.info("Crawling has started. Input = ${config.initialInputs}")

            dispatchEvent(
                CrawlingSession.Event.CrawlingStarted(
                    sessionId = sessionId,
                    initialInputs = config.initialInputs,
                )
            )

            try {
                val crawlingContext = beginTraversal()
                val crawlingDuration = crawlingDuration(startTime)
                val resultId = saveResult(crawlingContext, crawlingDuration)

                log.info("Crawling succeeded. Result.id = $resultId")

                dispatchEvent(
                    CrawlingSession.Event.CrawlingFinished(
                        resultId = resultId,
                        sessionId = sessionId,
                        initialInputs = config.initialInputs,
                        crawlingDuration = crawlingDuration,
                    )
                )
            } catch (error: Throwable) {
                log.error("Crawling failed unexpectedly", error)

                dispatchEvent(
                    CrawlingSession.Event.CrawlingFailed(
                        error = error,
                        sessionId = sessionId,
                        initialInputs = config.initialInputs,
                        crawlingDuration = crawlingDuration(startTime),
                    )
                )
            }
        }
    }

    override fun destroy() {
        try {
            sessionScope.cancel()
        } catch (error: Throwable) {
            // do nothing.
        }

        eventListener = null
    }

    private suspend fun beginTraversal(): CrawlingContext = coroutineScope {
        val crawlingContext = MutableCrawlingContext()
        val crawlFrontier = config.traversalAlgorithm.createCrawlFrontier<TraversalData<CrawlingInput>>()

        config.initialInputs.forEach { initialInput ->
            crawlFrontier.add(TraversalData(0, CrawlingInput(Source("root", "root"), initialInput)))
        }

        while (!crawlFrontier.isEmpty()) {
            val inputs = crawlFrontier.remove(config.inputProcessingBatchSize)
            val crawlingOutputs = processInputs(inputs, crawlingContext)

            for (crawlingOutput in crawlingOutputs) {
                crawlingContext.push(crawlingOutput.value.toCrawlingOutput())

                if (crawlingOutput.crawlDepth < config.maxCrawlDepth) {
                    crawlingOutput.toCrawlingInput()
                        .forEach(crawlFrontier::add)
                }
            }
        }

        crawlingContext
    }

    private fun TraversalData<InputProcessor.Output>.toCrawlingInput(): List<TraversalData<CrawlingInput>> {
        return this.value.data.map { dataPiece -> dataPiece.values }
            .flatten()
            .filter { propValue -> propValue.isCrawlable }
            .map { propValue -> TraversalData(this.crawlDepth, CrawlingInput(this.value.source, propValue.value)) }
    }

    private fun InputProcessor.Output.toCrawlingOutput(): CrawlingOutput {
        return CrawlingOutput(
            source = source,
            startedBy = startedBy,
            input = input,
            data = data.map { dataPiece -> dataPiece.mapValues { (_, value) -> value.value } },
            timestamp = timestamp,
        )
    }

    private suspend fun processInputs(
        inputs: List<TraversalData<CrawlingInput>>,
        crawlingContext: CrawlingContext
    ): List<TraversalData<InputProcessor.Output>> = coroutineScope {
        inputs.map { input -> async { processInput(input, crawlingContext) } }
            .awaitAll()
            .flatten()
    }

    private suspend fun processInput(
        input: TraversalData<CrawlingInput>,
        crawlingContext: CrawlingContext
    ): List<TraversalData<InputProcessor.Output>> = coroutineScope {
        config.inputProcessors.map { processor ->
            async {
                processor.processInputIfPossible(
                    input.value,
                    crawlingContext
                )
            }
        }
            .awaitAll()
            .flatten()
            .map { output -> TraversalData((input.crawlDepth + 1), output) }
    }

    private suspend fun InputProcessor.processInputIfPossible(
        input: CrawlingInput,
        context: CrawlingContext
    ): List<InputProcessor.Output> = withContext(config.coroutineDispatcher) {
        val processorInfoString = "InputProcessor(tag = ${source})"
        val inputInfoString = "CrawlingInput(input = ${input.rawInput}, source = ${input.source})"

        if (didProcess(input)) {
            log.info("$processorInfoString: has already processed the ${inputInfoString} before. Skipping the processing stage.")
            return@withContext emptyList()
        }

        val collectedOutput = mutableListOf<InputProcessor.Output>()

        try {
            log.info("probing the $processorInfoString with $inputInfoString")

            if (canProcess(input, context)) {
                config.throttler.throttle(this@processInputIfPossible)

                log.info("$processorInfoString: processing the ${inputInfoString}...")

                when (val result = process(input, context)) {
                    is Result.Success -> collectedOutput.add(result.output)
                    is Result.Failure -> {
                        log.warn("$processorInfoString: failed to process the $inputInfoString", result.error)
                    }
                }

                trackAsProcessed(input)
            }
        } catch (error: Throwable) {
            log.error("$processorInfoString: failed to process the $inputInfoString", error)
        }

        collectedOutput
    }

    private fun InputProcessor.trackAsProcessed(input: CrawlingInput) {
        processedInputsTracker.add(inputKey(this, input))
    }

    private fun InputProcessor.didProcess(input: CrawlingInput): Boolean {
        return (inputKey(this, input) in processedInputsTracker)
    }

    private fun inputKey(processor: InputProcessor, input: CrawlingInput): String {
        return "${processor.javaClass.canonicalName}(${input.rawInput})[${processor.source}]"
    }

    private suspend fun saveResult(
        context: CrawlingContext,
        crawlingDuration: CrawlingDuration
    ): String = withContext(config.coroutineDispatcher) {
        val result = CrawlingResult(
            initialInputs = config.initialInputs,
            context = context,
            crawlingDuration = crawlingDuration,
        )

        config.resultStore.save(result)
    }

    private fun dispatchEvent(event: CrawlingSession.Event) {
        eventListener?.invoke(event)
    }

    private fun crawlingDuration(startTimeMs: Long): CrawlingDuration {
        return CrawlingDuration(
            startTimeMs = startTimeMs,
            endTimeMs = System.currentTimeMillis(),
        )
    }

}
