package hk.yesdao.calendar.cache

import hk.yesdao.calendar.model.SolarTerm
import hk.yesdao.calendar.time.ShanghaiTimeResolver.SHANGHAI
import hk.yesdao.calendar.time.ShanghaiTimeResolver.ZI_BOUNDARY
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * SolarTermRangeCache is a global object; its cache lives for the whole JVM.
 *
 * Years that must be built ONLY by their own tests (otherwise the behaviour under test is not reproduced):
 *   1955, 1960 — cache filling
 *   1970, 1979–1980, 1989–1990 — lazy building by absolute index
 * All other tests use 2000–2050 (plus 2014 for the reference table).
 */
class SolarTermRangeCacheTest {

    private val dongZhi = SolarTerm.fromHanzi("冬至")!!
    private val xiaoHan = SolarTerm.fromHanzi("小寒")!!

    /** First term in 2000–2050 that starts at 23:xx: by midnight day D is already its, by Zi it still belongs to the previous term. */
    private fun termStartingAfterZiBoundary(): SolarTermRange =
        (2000..2050).asSequence()
            .flatMap { SolarTermRangeCache.getForYear(it).asSequence() }
            .firstOrNull { it.start.toLocalTime() >= ZI_BOUNDARY }
            ?: error("No solar term starting after 23:00 found")

    private fun shanghai(date: LocalDate, time: LocalTime) =
        date.atTime(time).atZone(SHANGHAI).toInstant()

    // ─────────────────────────────────────────────────────────────
    // 1. Cache filling
    // ─────────────────────────────────────────────────────────────
    @Nested
    inner class Filling {

        @Test
        fun `year is built once and then served from cache`() {
            val first = SolarTermRangeCache.getForYear(1960)
            val second = SolarTermRangeCache.getForYear(1960)

            assertSame(first, second, "Second call must return the cached list, not rebuild it")
        }

        @Test
        fun `concurrent requests for an unbuilt year get the same instance`() {
            val pool = Executors.newFixedThreadPool(8)
            try {
                val results = (1..16)
                    .map { pool.submit(Callable { SolarTermRangeCache.getForYear(1955) }) }
                    .map { it.get() }

                assertTrue(results.all { it === results[0] }, "Year was built more than once")
            } finally {
                pool.shutdown()
            }
        }

        @Test
        fun `built year has the expected structure`() {
            for (year in 2000..2050) {
                val ranges = SolarTermRangeCache.getForYear(year)

                assertEquals(24, ranges.size, "year $year")
                assertEquals(24, ranges.map { it.term }.toSet().size, "Duplicate terms in $year")
                assertEquals(xiaoHan, ranges.first().term, "First term of $year")
                assertEquals(dongZhi, ranges.last().term, "Last term of $year")

                ranges.forEach { r ->
                    assertEquals(year, r.start.year, "${r.term} $year")
                    assertTrue(r.start < r.end, "${r.term} $year: start >= end")
                    assertTrue(r.ziStart <= r.ziEnd, "${r.term} $year: ziStart > ziEnd")
                }
                ranges.zipWithNext().forEach { (a, b) ->
                    assertEquals(b.start, a.end, "${a.term} → ${b.term} in $year: end != start of next term")
                }

                // The last term of a year ends where the first term of the next year starts
                val nextFirst = SolarTermRangeCache.getForYear(year + 1).first()
                assertEquals(nextFirst.start, ranges.last().end, "Boundary $year → ${year + 1}")
            }
        }

        /**
         * Check against an external source. All other tests check the cache against itself;
         * only this one catches shifted moments (time zone, error in JieQiCache).
         *
         * Source: Hong Kong Observatory, "Date and Time information of the 24 Solar Terms in 2014",
         * https://www.hko.gov.hk/en/gts/astron2014/Solar_Term_2014.htm
         * Times are Hong Kong Time (UTC+8), same as Beijing time. HKO gives minutes → ±1 min tolerance for rounding.
         * Order follows the table, from 小寒 to 冬至.
         */
        @Test
        fun `2014 term moments match Hong Kong Observatory`() {
            fun t(month: Int, day: Int, hour: Int, minute: Int) = LocalDateTime.of(2014, month, day, hour, minute)

            val hko = listOf(
                t(1, 5, 18, 24),   // Moderate cold    小寒
                t(1, 20, 11, 51),  // Severe cold      大寒
                t(2, 4, 6, 3),     // Spring commences 立春
                t(2, 19, 1, 59),   // Spring showers   雨水
                t(3, 6, 0, 2),     // Insects waken    惊蛰
                t(3, 21, 0, 57),   // Vernal equinox   春分
                t(4, 5, 4, 47),    // Bright and clear 清明
                t(4, 20, 11, 56),  // Corn rain        谷雨
                t(5, 5, 21, 59),   // Summer commences 立夏
                t(5, 21, 10, 59),  // Corn forms       小满
                t(6, 6, 2, 3),     // Corn on ear      芒种
                t(6, 21, 18, 51),  // Summer solstice  夏至
                t(7, 7, 12, 15),   // Moderate heat    小暑
                t(7, 23, 5, 41),   // Great heat       大暑
                t(8, 7, 22, 2),    // Autumn commences 立秋
                t(8, 23, 12, 46),  // End of heat      处暑
                t(9, 8, 1, 1),     // White dew        白露
                t(9, 23, 10, 29),  // Autumnal equinox 秋分
                t(10, 8, 16, 47),  // Cold dew         寒露
                t(10, 23, 19, 57), // Frost            霜降
                t(11, 7, 20, 7),   // Winter commences 立冬
                t(11, 22, 17, 38), // Light snow       小雪
                t(12, 7, 13, 4),   // Heavy snow       大雪
                t(12, 22, 7, 3),   // Winter solstice  冬至
            )

            val ranges = SolarTermRangeCache.getForYear(2014)
            assertEquals(hko.size, ranges.size)

            ranges.zip(hko).forEach { (r, expected) ->
                val diff = Duration.between(expected, r.start).abs()
                assertTrue(
                    diff <= Duration.ofMinutes(1),
                    "${r.term}: cache ${r.start}, HKO $expected (diff $diff)"
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 2. Absolute index and years outside the warm-up window
    // ─────────────────────────────────────────────────────────────
    @Nested
    inner class OutsideWarmWindow {

        @Test
        fun `previous term is found in a year that was never built`() {
            // Builds only 1990; 1989 is not in the cache
            val current = SolarTermRangeCache.getSolarTermRange(LocalDate.of(1990, 1, 10))
            assertEquals(xiaoHan, current.term)

            val previous = SolarTermRangeCache.findByAbsoluteIndex(current.absoluteIndex - 1)

            assertNotNull(previous, "1989 was never built — the old byAbsoluteIndex lookup returned null")
            assertEquals(dongZhi, previous!!.term)
            assertEquals(current.ziStart, previous.ziEnd.plusDays(1))
        }

        @Test
        fun `findByAbsoluteIndex builds the year lazily`() {
            val index = 1970L * 24 + 5

            val range = SolarTermRangeCache.findByAbsoluteIndex(index)

            assertNotNull(range, "1970 is not in the cache")
            assertEquals(index, range!!.absoluteIndex)
        }

        @Test
        fun `findByAbsoluteIndexRange builds years lazily across the year boundary`() {
            val from = 1979L * 24 + 22
            val to = 1980L * 24 + 1

            val ranges = SolarTermRangeCache.findByAbsoluteIndexRange(from, to)

            assertEquals((from..to).toList(), ranges.map { it.absoluteIndex })
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 3. Index
    // ─────────────────────────────────────────────────────────────
    @Nested
    inner class Index {

        @Test
        fun `absolute index crosses the year boundary`() {
            val lastOf2025 = SolarTermRangeCache.findByAbsoluteIndex(2026L * 24 - 1)!!
            val firstOf2026 = SolarTermRangeCache.findByAbsoluteIndex(2026L * 24)!!

            assertEquals(dongZhi, lastOf2025.term)
            assertEquals(xiaoHan, firstOf2026.term)
            assertEquals(lastOf2025.ziEnd.plusDays(1), firstOf2026.ziStart)
        }

        @Test
        fun `position in year == absoluteIndex, Zi days are contiguous`() {
            for (year in 2000..2050) {
                val ranges = SolarTermRangeCache.getForYear(year)

                ranges.forEachIndexed { i, r ->
                    assertEquals(year.toLong() * 24 + i, r.absoluteIndex, "year $year, position $i")
                    assertEquals(r, SolarTermRangeCache.findByAbsoluteIndex(r.absoluteIndex))
                }
                ranges.zipWithNext().forEach { (a, b) ->
                    assertEquals(a.ziEnd.plusDays(1), b.ziStart, "Gap between ${a.term} and ${b.term} in $year")
                }
            }
        }

        @Test
        fun `previous term inside the year`() {
            val ranges = SolarTermRangeCache.getForYear(2030)

            assertEquals(
                2030 to ranges[4].term,
                SolarTermRangeCache.getPreviousSolarTerm(2030, ranges[5].term)
            )
        }

        @Test
        fun `previous term of 小寒 is 冬至 of the previous year`() {
            assertEquals(2029 to dongZhi, SolarTermRangeCache.getPreviousSolarTerm(2030, xiaoHan))
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 4. Lookup by date / by term
    // ─────────────────────────────────────────────────────────────
    @Nested
    inner class Lookup {

        @Test
        fun `lookup by year and term returns the cached range`() {
            SolarTermRangeCache.getForYear(2030).forEach { r ->
                assertEquals(r, SolarTermRangeCache.getSolarTermRange(2030, r.term))
            }
        }

        @Test
        fun `every day of a year belongs to exactly the term that contains it`() {
            var day = LocalDate.of(2030, 1, 1)
            while (day.year == 2030) {
                val r = SolarTermRangeCache.getSolarTermRange(day)
                assertTrue(day in r.ziStart..r.ziEnd, "$day is outside ${r.term} ${r.ziStart}..${r.ziEnd}")
                day = day.plusDays(1)
            }
        }

        @Test
        fun `first days of January belong to previous year's 冬至`() {
            val r = SolarTermRangeCache.getSolarTermRange(LocalDate.of(2030, 1, 1))

            assertEquals(dongZhi, r.term)
            assertEquals(2029L * 24 + 23, r.absoluteIndex)
        }

        @Test
        fun `calendar day of a term starting after 23_00 belongs to the previous term`() {
            val r = termStartingAfterZiBoundary()
            val calendarDay = r.start.toLocalDate()

            assertEquals(calendarDay.plusDays(1), r.ziStart)
            assertNotEquals(r.term, SolarTermRangeCache.getSolarTermRange(calendarDay).term)
            assertEquals(r.term, SolarTermRangeCache.getSolarTermRange(r.ziStart).term)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 5. resolveActiveSolarTerm(Instant) — active term by Zi day
    // ─────────────────────────────────────────────────────────────
    @Nested
    inner class ActiveSolarTerm {

        @Test
        fun `middle of a term`() {
            val r = SolarTermRangeCache.getForYear(2030)[5]
            val instant = shanghai(r.ziStart.plusDays(3), LocalTime.NOON)

            assertEquals(r, SolarTermRangeCache.resolveActiveSolarTerm(instant))
        }

        /** The term starts at 23:xx. At 23:00 its exact moment has not come yet, but the Zi day already belongs to it. */
        @Test
        fun `after 23_00 the new term is active even before its exact moment`() {
            val r = termStartingAfterZiBoundary()
            val day = r.start.toLocalDate()

            assertEquals(r, SolarTermRangeCache.resolveActiveSolarTerm(shanghai(day, ZI_BOUNDARY)))

            val before = SolarTermRangeCache.resolveActiveSolarTerm(shanghai(day, LocalTime.of(22, 59)))
            assertEquals(r.absoluteIndex - 1, before.absoluteIndex)
        }

        /** 15:30 UTC = 23:30 Shanghai → already the next Zi day. If computed in UTC, the old term would remain. */
        @Test
        fun `instant is interpreted in Shanghai time`() {
            val r = SolarTermRangeCache.getForYear(2030)[3]
            val instant = r.ziEnd.atTime(15, 30).toInstant(ZoneOffset.UTC)

            assertEquals(r.absoluteIndex + 1, SolarTermRangeCache.resolveActiveSolarTerm(instant).absoluteIndex)
        }

        @Test
        fun `early January belongs to previous year's 冬至`() {
            val active = SolarTermRangeCache.resolveActiveSolarTerm(shanghai(LocalDate.of(2030, 1, 2), LocalTime.NOON))

            assertEquals(dongZhi, active.term)
            assertEquals(2029L * 24 + 23, active.absoluteIndex)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 6. Free access boundary (moves to yesdao-platform together with the method)
    // ─────────────────────────────────────────────────────────────
    @Nested
    inner class FreeAccessBoundary {

        private val day = LocalDate.of(2030, 6, 10)

        @Test
        fun `before 23_00 the boundary is two days back`() {
            assertEquals(day.minusDays(2), SolarTermRangeCache.resolveFreeAccessBoundary(day.atTime(22, 59)))
            assertEquals(day.minusDays(2), SolarTermRangeCache.resolveFreeAccessBoundary(day.atStartOfDay()))
        }

        @Test
        fun `from 23_00 the boundary moves one day forward`() {
            assertEquals(day.minusDays(1), SolarTermRangeCache.resolveFreeAccessBoundary(day.atTime(23, 0)))
            assertEquals(day.minusDays(1), SolarTermRangeCache.resolveFreeAccessBoundary(day.atTime(23, 59)))
        }
    }
}