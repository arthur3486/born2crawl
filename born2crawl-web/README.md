# born2crawl-web

[![Download](https://img.shields.io/maven-central/v/com.arthurivanets/born2crawl-web.svg?label=Download)](https://mvnrepository.com/search?q=com.arthurivanets.born2crawl-data-store)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build](https://github.com/arthur3486/born2crawl/workflows/Build/badge.svg?branch=main)](https://github.com/arthur3486/born2crawl/actions)

**born2crawl-web** provides a set of concrete implementations of the [`InputProcessor`](../born2crawl-core/src/main/java/com/arthurivanets/born2crawl/InputProcessor.kt)
that can be used for web crawling.

## Installation

***Latest version:*** [![Download](https://img.shields.io/maven-central/v/com.arthurivanets/born2crawl-web.svg?label=Download)](https://mvnrepository.com/search?q=com.arthurivanets.born2crawl-core)

```groovy
dependencies {
    implementation("com.arthurivanets:born2crawl-web:x.y.z")
}
```

## External Dependencies

**born2crawl-web** depends on the following external dependencies:

* [`OkHttp`](https://github.com/square/okhttp) - HTTP client for the JVM, Android, and GraalVM.
* [`jsoup`](https://github.com/jhy/jsoup) - the Java HTML parser, built for HTML editing, cleaning, scraping, and XSS safety.

## Available Input Processor Implementations

* [`WebPageCrawler`](src/main/java/com/arthurivanets/born2crawl/web/WebPageCrawler.kt) - a concrete implementation of the
  [`InputProcessor`](../born2crawl-core/src/main/java/com/arthurivanets/born2crawl/InputProcessor.kt) that allows to crawl the web pages.
* [`FileDownloader`](src/main/java/com/arthurivanets/born2crawl/web/FileDownloader.kt) - a concrete implementation of the
  [`InputProcessor`](../born2crawl-core/src/main/java/com/arthurivanets/born2crawl/InputProcessor.kt) that allows to download files by urls.
