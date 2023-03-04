# born2crawl-data-store

[![Download](https://img.shields.io/maven-central/v/com.arthurivanets/born2crawl-core.svg?label=Download)](https://mvnrepository.com/search?q=com.arthurivanets.born2crawl-data-store)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build](https://github.com/arthur3486/born2crawl/workflows/Build/badge.svg?branch=main)](https://github.com/arthur3486/born2crawl/actions)

**born2crawl-data-store** provides the concrete implementation of the [`CrawlingResultStore`](../born2crawl-core/src/main/java/com/arthurivanets/born2crawl/CrawlingResultStore.kt)
that stores the crawling results in json files.

## Installation

***Latest version:*** [![Download](https://img.shields.io/maven-central/v/com.arthurivanets/born2crawl-data-store.svg?label=Download)](https://mvnrepository.com/search?q=com.arthurivanets.born2crawl-core)

```groovy
dependencies {
    implementation("com.arthurivanets:born2crawl-data-store:x.y.z")
}
```

## External Dependencies

**born2crawl-data-store** depends on the following external dependencies:

* [`Gson`](https://github.com/google/gson) - a Java JSON serialization/deserialization library.

## Result File Format

[`FileCrawlingResultStore`](src/main/java/com/arthurivanets/born2crawl/stores/FileCrawlingResultStore.kt)
stores the
[`CrawlingResult`](../born2crawl-core/src/main/java/com/arthurivanets/born2crawl/CrawlingResult.kt)
into json files that have the following format:

```json
{
  "initialInputs":[
    "https://en.wikipedia.org/wiki/Microprocessor"
  ],
  "outputs":[
    {
      "source":{
        "name":"web-crawler",
        "id":"2E521ED3-9554-4391-8A93-14F32376BD6B"
      },
      "startedBy":{
        "name":"root",
        "id":"root"
      },
      "input":"https://en.wikipedia.org/wiki/Microprocessor",
      "data":[
        {
          "url":"https://en.wikipedia.org/wiki/Digital_integrated_circuit"
        },
        {
          "url":"https://en.wikipedia.org/wiki/Instruction_(computing)"
        },
        {
          "url":"https://en.wikipedia.org/wiki/Very-Large-Scale_Integration"
        }
      ],
      "timestamp":1676851690130
    }
  ],
  "crawlingStartTimeMs":1676851689303,
  "crawlingEndTimeMs":1676851692378
}
```
