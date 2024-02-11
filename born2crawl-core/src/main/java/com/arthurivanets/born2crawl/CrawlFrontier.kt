package com.arthurivanets.born2crawl

import java.util.*

/**
 * Creates a new [CrawlFrontier] data structure that conforms to the specified data graph traversal algorithm.
 */
internal fun <T> TraversalAlgorithm.createCrawlFrontier(): CrawlFrontier<T> {
    return when (this) {
        TraversalAlgorithm.BFS -> BFSCrawlFrontier()
        TraversalAlgorithm.DFS -> DFSCrawlFrontier()
    }
}

/**
 * A contract for the implementation of concrete crawl frontier data structures.
 */
internal interface CrawlFrontier<T> {

    /**
     * Adds the provided item to the current frontier.
     *
     * @param item the item to be added to the frontier.
     */
    fun add(item: T)

    /**
     * Removes the item from the current frontier (based on the internal implementation of the frontier).
     *
     * @return the removed item, or null if the frontier is empty.
     */
    fun remove(): T?

    /**
     * Returns true if the current frontier contains no elements.
     */
    fun isEmpty(): Boolean

}

private class DFSCrawlFrontier<T> : CrawlFrontier<T> {

    private val items = LinkedList<T>()

    override fun add(item: T) {
        items.push(item)
    }

    override fun remove(): T? {
        return if (items.isNotEmpty()) items.pop() else null
    }

    override fun isEmpty(): Boolean {
        return items.isEmpty()
    }

}

private class BFSCrawlFrontier<T> : CrawlFrontier<T> {

    private val items = LinkedList<T>()

    override fun add(item: T) {
        items.offer(item)
    }

    override fun remove(): T? {
        return if (items.isNotEmpty()) items.poll() else null
    }

    override fun isEmpty(): Boolean {
        return items.isEmpty()
    }

}
