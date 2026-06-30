package hk.yesdao.calendar.cache

import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe cache of Earth transition date ranges.
 * 18-day seasonal transition model used by this calendar engine.
 * 「四季者，指立春、立夏、立秋、立冬之前各十八天。」
 *
 * Each transition period starts 18 days before one of the four seasonal
 * solar terms (Beginning of Spring, Summer, Autumn and Winter).
 *
 * These periods are used by the calendar engine to determine Earth days.
 */
object EarthTransitionCache {

    private val cache = ConcurrentHashMap<Int, List<DateRange>>()

    /**
     * Seasonal solar terms marking Earth transition periods.
     */
    private val EARTH_TRANSITION_TERMS = listOf(
        "立春", // Beginning of Spring
        "立夏", // Beginning of Summer
        "立秋", // Beginning of Autumn
        "立冬"  // Beginning of Winter
    )

    /**
     * Builds Earth transition date ranges for the specified Gregorian year.
     */
    private fun buildEarthTransitions(year: Int): List<DateRange> {
        val jieQi = JieQiCache.getForYear(year)

        return EARTH_TRANSITION_TERMS.mapNotNull { name ->
            val solar = jieQi[name] ?: return@mapNotNull null
            val jieDate = LocalDate.of(solar.year, solar.month, solar.day)

            DateRange(
                start = jieDate.minusDays(18),
                end = jieDate
            )
        }
    }

    /**
     * Returns true if the specified date belongs to
     * one of the Earth transition periods.
     */
    fun isEarthDay(date: LocalDate): Boolean =
        getForYear(date.year)
            .any { date >= it.start && date < it.end }

    private fun getForYear(year: Int): List<DateRange> =
        cache.computeIfAbsent(year) {
            buildEarthTransitions(year)
        }
}

/**
 * Half-open date range.
 *
 * start is inclusive, end is exclusive.
 */
data class DateRange(
    val start: LocalDate,
    val end: LocalDate
)