package hk.yesdao.calendar.cache

import hk.yesdao.calendar.exceptionHandling.ResourceNotFoundException
import hk.yesdao.calendar.model.SolarTerm
import hk.yesdao.calendar.time.ShanghaiTimeResolver.SHANGHAI
import hk.yesdao.calendar.time.ShanghaiTimeResolver.now
import hk.yesdao.calendar.time.ShanghaiTimeResolver.resolveZiDate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache of the 24 solar terms (节气) as ranges of time and of Zi days.
 *
 * Each Gregorian year holds exactly 24 terms, from 小寒 (early January) to 冬至 (late December).
 * A term is identified by its absolute index `year * 24 + position`, which makes it easy
 * to step across year boundaries (`index - 1` of 小寒 is 冬至 of the previous year).
 *
 * Day boundaries follow the Zi-hour rule: the calendrical day changes at 23:00 Asia/Shanghai,
 * so a term starting at 23:30 owns the following day, not the current one.
 *
 * Years around the current one are built at startup; any other year is built lazily on first access.
 */
object SolarTermRangeCache {

    private const val WARM_BACK = 10
    private const val WARM_FORWARD = 10

    private val cache = ConcurrentHashMap<Int, List<SolarTermRange>>()

    init {
        val year = now().year
        for (y in (year - WARM_BACK)..(year + WARM_FORWARD)) getForYear(y)
    }

    /** All 24 terms of [year], in chronological order. Built once, then served from cache. */
    fun getForYear(year: Int): List<SolarTermRange> =
        cache.computeIfAbsent(year) { buildRangesForYear(it) }

    /**
     * The term that contains the given Zi day.
     * Early January days before 小寒 belong to 冬至 of the previous year.
     */
    fun getSolarTermRange(date: LocalDate): SolarTermRange =
        getForYear(date.year).firstOrNull { date in it.ziStart..it.ziEnd }
            ?: getForYear(date.year - 1).firstOrNull { date in it.ziStart..it.ziEnd }
            ?: throw ResourceNotFoundException("Solar term not found for date=$date")

    /** The given [term] of [year]. */
    fun getSolarTermRange(year: Int, term: SolarTerm): SolarTermRange =
        getForYear(year).firstOrNull { it.term == term }
            ?: throw ResourceNotFoundException("Solar term $term not found for year $year")

    /** The term active at [instant], decided by its Zi day in Asia/Shanghai. */
    fun resolveActiveSolarTerm(instant: Instant): SolarTermRange =
        getSolarTermRange(resolveZiDate(instant.atZone(SHANGHAI)))

    /** The term preceding [term] of [year], as (year, term). Crosses year boundaries. */
    fun getPreviousSolarTerm(year: Int, term: SolarTerm): Pair<Int, SolarTerm> {
        val previous = findByAbsoluteIndex(getSolarTermRange(year, term).absoluteIndex - 1)
            ?: error("Previous solar term not found for $year $term")
        return Math.floorDiv(previous.absoluteIndex, 24L).toInt() to previous.term
    }

    /** The term with the given absolute index (`year * 24 + position`). Builds the year if needed. */
    fun findByAbsoluteIndex(index: Long): SolarTermRange? {
        val year = Math.floorDiv(index, 24L).toInt()
        val position = Math.floorMod(index, 24L).toInt()
        return getForYear(year).getOrNull(position)
            ?.takeIf { it.absoluteIndex == index }
    }

    /** Terms with absolute indices in [fromIndex]..[toIndex], inclusive. */
    fun findByAbsoluteIndexRange(fromIndex: Long, toIndex: Long): List<SolarTermRange> =
        (fromIndex..toIndex).mapNotNull { findByAbsoluteIndex(it) }

    /**
     * Last day open for free access at [now] (Asia/Shanghai local time):
     * two Zi days back, so the previous day opens at 23:00.
     */
    fun resolveFreeAccessBoundary(now: LocalDateTime): LocalDate =
        resolveZiDate(now).minusDays(2)

    private fun buildRangesForYear(year: Int): List<SolarTermRange> {
        val terms = termStarts(year)
        check(terms.size == 24) { "Expected 24 solar terms for $year, got ${terms.size}" }
        val nextYearFirst = termStarts(year + 1).firstOrNull()?.second
            ?: error("No solar term data for ${year + 1}")

        return terms.mapIndexed { index, (term, start) ->
            val end = terms.getOrNull(index + 1)?.second ?: nextYearFirst
            SolarTermRange(
                term = term,
                start = start,
                end = end,
                ziStart = resolveZiDate(start.atZone(SHANGHAI)),
                // The Zi day of the transition already belongs to the next term
                ziEnd = resolveZiDate(end.atZone(SHANGHAI)).minusDays(1),
                absoluteIndex = year.toLong() * 24 + index
            )
        }
    }

    /** Start moments of the terms that fall within [year], sorted chronologically. */
    private fun termStarts(year: Int): List<Pair<SolarTerm, LocalDateTime>> =
        JieQiCache.getForYear(year).mapNotNull { (rawKey, solar) ->
            val term = SolarTerm.fromEnumName(rawKey) ?: SolarTerm.fromHanzi(rawKey) ?: return@mapNotNull null
            val start = LocalDateTime.parse(solar.toYmdHms(), YMD_HMS_FORMATTER)
            if (start.year == year) term to start else null
        }.sortedBy { it.second }

    /**
     * First Zi day of the term preceding the one active at [now].
     * [now] may be in any time zone; it is converted to Asia/Shanghai.
     */
    fun getPreviousSolarTermStart(now: ZonedDateTime): LocalDate {
        val current = resolveActiveSolarTerm(now.toInstant())
        val previous = findByAbsoluteIndex(current.absoluteIndex - 1)
            ?: error("Previous solar term not found for index ${current.absoluteIndex}")
        return previous.ziStart
    }
}

private val YMD_HMS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

/**
 * One solar term.
 *
 * @property start astronomical moment the term begins (Asia/Shanghai local time)
 * @property end astronomical moment the next term begins (exclusive)
 * @property ziStart first Zi day of the term
 * @property ziEnd last Zi day of the term (inclusive)
 * @property absoluteIndex `year * 24 + position`, continuous across years
 */
data class SolarTermRange(
    val term: SolarTerm,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val ziStart: LocalDate,
    val ziEnd: LocalDate,
    val absoluteIndex: Long
)