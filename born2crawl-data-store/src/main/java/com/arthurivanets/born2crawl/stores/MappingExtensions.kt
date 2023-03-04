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

package com.arthurivanets.born2crawl.stores

import com.arthurivanets.born2crawl.*
import com.arthurivanets.born2crawl.stores.model.FileCrawlingOutput
import com.arthurivanets.born2crawl.stores.model.FileCrawlingResult
import com.arthurivanets.born2crawl.stores.model.FileSource

internal fun CrawlingResult.toFileModel(): FileCrawlingResult {
    return FileCrawlingResult(
        initialInputs = this.initialInputs.toList(),
        outputs = this.context.getAll().map(CrawlingOutput::toFileModel),
        crawlingStartTimeMs = this.crawlingDuration.startTimeMs,
        crawlingEndTimeMs = this.crawlingDuration.endTimeMs,
    )
}

internal fun FileCrawlingResult.toDomainModel(): CrawlingResult {
    return CrawlingResult(
        initialInputs = this.initialInputs.toSet(),
        context = this.outputs.toCrawlingContext(),
        crawlingDuration = CrawlingDuration(
            startTimeMs = this.crawlingStartTimeMs,
            endTimeMs = this.crawlingEndTimeMs,
        )
    )
}

private fun List<FileCrawlingOutput>.toCrawlingContext(): CrawlingContext {
    val context = MutableCrawlingContext()
    context.push(this.map(FileCrawlingOutput::toDomainModel))

    return context.readOnly()
}

private fun CrawlingOutput.toFileModel(): FileCrawlingOutput {
    return FileCrawlingOutput(
        source = this.source.toFileModel(),
        startedBy = this.startedBy.toFileModel(),
        input = this.input,
        data = this.data,
        timestamp = this.timestamp,
    )
}

private fun FileCrawlingOutput.toDomainModel(): CrawlingOutput {
    return CrawlingOutput(
        source = this.source.toDomainModel(),
        startedBy = this.startedBy.toDomainModel(),
        input = this.input,
        data = this.data,
        timestamp = this.timestamp,
    )
}

private fun Source.toFileModel(): FileSource {
    return FileSource(
        name = this.name,
        id = this.id,
    )
}

private fun FileSource.toDomainModel(): Source {
    return Source(
        name = this.name,
        id = this.id,
    )
}