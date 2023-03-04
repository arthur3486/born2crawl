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

import com.arthurivanets.born2crawl.CrawlingResult
import com.arthurivanets.born2crawl.CrawlingResultStore
import com.arthurivanets.born2crawl.stores.model.FileCrawlingResult
import com.arthurivanets.born2crawl.util.UniqueIds
import com.google.gson.Gson
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * A concrete implementation of the [CrawlingResultStore] that stores the crawling results in files.
 *
 * @param outputDir the directory into which the result files will be written.
 */
class FileCrawlingResultStore(outputDir: File) : CrawlingResultStore {

    private val gson = Gson()
    private val resultsOutputDir by lazy { createResultsOutputDirectory(outputDir) }

    init {
        require(outputDir.isDirectory) { "Please, provide a valid output directory. Provided: $outputDir" }
        require(outputDir.isWritable()) { "Please, ensure that the output directory has write permissions." }
    }

    override fun save(result: CrawlingResult): String {
        val resultId = normalizeId(UniqueIds.random())

        createResultFile(resultId).writeCrawlingResult(result)

        return resultId
    }

    override fun getById(id: String): CrawlingResult? {
        val normalizedId = normalizeId(id)

        require(normalizedId.isNotBlank()) { "Please, provide a valid result id. Provided: $id" }

        return resultFile(id)?.readCrawlingResult()
    }

    override fun getAll(): List<CrawlingResult> {
        return (resultsOutputDir.listFiles() ?: emptyArray<File>())
            .filter { file -> file.isCrawlingResultFile() }
            .map { file -> file.readCrawlingResult() }
    }

    override fun deleteById(id: String) {
        val normalizedId = normalizeId(id)

        require(normalizedId.isNotBlank()) { "Please, provide a valid result id. Provided: $id" }

        resultFile(id)?.delete()
    }

    override fun deleteAll() {
        resultsOutputDir.deleteRecursively()
        resultsOutputDir.mkdirs()
    }

    private fun normalizeId(rawId: String): String {
        return rawId.replace(Regex("[^a-zA-Z_0-9]"), "")
    }

    private fun createResultsOutputDirectory(parentDir: File): File {
        fun outputDir(index: Int): File {
            return File(parentDir, "results${if (index > 0) "-$index" else ""}")
        }

        fun File.isAcceptableDir(): Boolean {
            return (!exists() || (isDirectory && isWritable()))
        }

        var dirIndex = 0

        while (!outputDir(dirIndex).isAcceptableDir()) {
            dirIndex++
        }

        return outputDir(dirIndex)
            .also(File::mkdirs)
    }

    private fun resultFile(resultId: String): File? {
        return createResultFile(resultId)
            .let { file -> if (file.isCrawlingResultFile()) file else null }
    }

    private fun createResultFile(resultId: String): File {
        return File(resultsOutputDir, "$resultId.json")
    }

    private fun File.readCrawlingResult(): CrawlingResult {
        return FileReader(this)
            .use { reader -> gson.fromJson(reader, FileCrawlingResult::class.java) }
            .toDomainModel()
    }

    private fun File.writeCrawlingResult(result: CrawlingResult) {
        return FileWriter(this)
            .use { writer -> gson.toJson(result.toFileModel(), writer) }
    }

    private fun File.isWritable(): Boolean {
        return try {
            canWrite()
        } catch (e: SecurityException) {
            false
        }
    }

    private fun File.isCrawlingResultFile(): Boolean {
        val nameRegex = Regex("^[a-zA-Z_0-9]+\\.json$")
        return (this.exists() && this.isFile && this.name.matches(nameRegex))
    }

}