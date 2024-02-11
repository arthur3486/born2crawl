package com.arthurivanets.born2crawl

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class CrawlFrontierTests {

    @Test
    fun `bfs frontier - item operations`() {
        // BFS frontier - queue based [FIFO]
        val frontier = TraversalAlgorithm.BFS.createCrawlFrontier<Int>()

        assertTrue(frontier.isEmpty())
        assertNull(frontier.remove())

        frontier.add(1)

        assertFalse(frontier.isEmpty())

        frontier.add(2)
        frontier.add(3)

        // item removal
        assertEquals(1, frontier.remove())
        assertEquals(2, frontier.remove())
        assertEquals(3, frontier.remove())

        assertTrue(frontier.isEmpty())
        assertNull(frontier.remove())
    }

    @Test
    fun `dfs frontier - item operations`() {
        // DFS frontier - stack based [LIFO]
        val frontier = TraversalAlgorithm.DFS.createCrawlFrontier<Int>()

        assertTrue(frontier.isEmpty())
        assertNull(frontier.remove())

        frontier.add(1)

        assertFalse(frontier.isEmpty())

        frontier.add(2)
        frontier.add(3)

        // item removal
        assertEquals(3, frontier.remove())
        assertEquals(2, frontier.remove())
        assertEquals(1, frontier.remove())

        assertTrue(frontier.isEmpty())
        assertNull(frontier.remove())
    }

}
