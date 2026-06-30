package hk.yesdao.calendar.cache

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JieQiCacheTest {

    @AfterEach
    fun tearDown() {
        JieQiCache.clearAll()
    }

    @Test
    fun `should return solar terms for requested year`() {
        val terms = JieQiCache.getForYear(2025)

        assertFalse(terms.isEmpty())
    }

    @Test
    fun `should contain major solar terms`() {
        val terms = JieQiCache.getForYear(2025)

        assertAll(
            { assertTrue(terms.containsKey("立春")) },
            { assertTrue(terms.containsKey("春分")) },
            { assertTrue(terms.containsKey("夏至")) },
            { assertTrue(terms.containsKey("秋分")) },
            { assertTrue(terms.containsKey("冬至")) }
        )
    }

    @Test
    fun `should return same cached instance for same year`() {
        val first = JieQiCache.getForYear(2025)
        val second = JieQiCache.getForYear(2025)

        assertSame(first, second)
    }

    @Test
    fun `should rebuild cache after clearing specific year`() {
        val first = JieQiCache.getForYear(2025)

        JieQiCache.clearYear(2025)

        val second = JieQiCache.getForYear(2025)

        assertNotSame(first, second)
    }

    @Test
    fun `should rebuild cache after clearing all`() {
        val first = JieQiCache.getForYear(2025)

        JieQiCache.clearAll()

        val second = JieQiCache.getForYear(2025)

        assertNotSame(first, second)
    }

    @Test
    fun `should cache years independently`() {
        val year2024 = JieQiCache.getForYear(2024)
        val year2025 = JieQiCache.getForYear(2025)

        assertNotSame(year2024, year2025)
    }
}