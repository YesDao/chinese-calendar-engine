package hk.yesdao.calendar.converter

import com.nlf.calendar.Lunar
import com.nlf.calendar.LunarYear
import com.nlf.calendar.Solar
import hk.yesdao.calendar.cache.EarthTransitionCache.isEarthDay
import hk.yesdao.calendar.exceptionHandling.IllegalStateException
import hk.yesdao.calendar.model.*
import hk.yesdao.calendar.time.ShanghaiTimeResolver.SHANGHAI
import org.slf4j.LoggerFactory.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import kotlin.math.absoluteValue

/**
* Converts Gregorian dates into complete Chinese calendrical information.
*
* This converter is the main entry point of the calendar engine.
* It combines calculations for:
*
* - Lunar calendar
* - Heavenly Stems (天干)
* - Earthly Branches (地支)
* - Sexagenary cycle (干支)
* - Solar Terms (节气)
* - Leap months
* - Seasonal Qi
* - Forecast-specific calendar fields
*/
object LunarDateConverter {

    private val logger = getLogger(LunarDateConverter::class.java)

    /**
     * Converts a zoned Gregorian date and time into a complete LunarDate.
     *
     * This is the primary conversion method of the calendar engine.
     * All other overloads delegate to it.
     *
     * Day boundary follows the traditional Zi Hour (子时) rule:
     * the new calendrical day begins at **23:00**, not at midnight.
     * For example, 2024-05-20 23:30 (Asia/Shanghai) belongs to the
     * day pillar of 2024-05-21. See `docs/CALENDAR_RULES.md`.
     *
     * The resulting LunarDate contains:
     * - Four Pillars (BaZi)
     * - Lunar calendar date
     * - Solar Term
     * - Sexagenary day index (zero-based, consistent with the day pillar)
     * - Leap month information
     * - Forecast-specific month data
     * - Seasonal Qi information
     *
     * @param zoned Gregorian date and time with its timezone.
     * @return Complete Chinese calendrical representation.
     * @since 1.1.0
     */
    fun fromGregorian(zoned: ZonedDateTime): LunarDate {
        logger.debug("Input zoned date: {}", zoned)

        // Resolve lunar calendar and solar term data for the given instant.
        val shanghai = zoned.withZoneSameInstant(SHANGHAI)
        val solar = Solar.fromYmdHms(
            shanghai.year, shanghai.monthValue, shanghai.dayOfMonth,
            shanghai.hour, shanghai.minute, shanghai.second
        )

        val bazi = solar.lunar.eightChar
        logger.debug(
            "Calculated stems/branches: year={}{} month={}{} day={}{} hour={}{}",
            solar.lunar.yearGanExact, solar.lunar.yearZhiExact,
            solar.lunar.monthGanExact, solar.lunar.monthZhiExact,
            solar.lunar.dayGanExact, solar.lunar.dayZhiExact,
            solar.lunar.timeGan, solar.lunar.timeZhi
        )

        // Determine the currently active Solar Term (e.g. 立春, 雨水...).
        val solarTerm = getActiveSolarTerm(solar.lunar)

        // Zero-based position of the day within the sexagenary cycle (0..59).
        // Uses the Zi Hour (子时) boundary, consistent with the day pillar.
        val dayIndexInCycle = GanZhi.indexOf(solar.lunar.dayInGanZhiExact)

        // --- Forecast-specific fields used by background layers ---
        val isLeapYear = java.time.Year.isLeap(shanghai.year.toLong())

        // Month pillar (干支) used for forecasting, adjusted for leap months (闰月).
        val (monthStemForForecast, monthBranchForForecast) = getEffectiveMonthGanZhi(solar.lunar)

        // Month pillar for Tai Sui (太岁), determined strictly by Solar Terms.
        val (monthStemForTaisui, monthBranchForTaisui) = getMonthGanZhiBySolarTerm(solar.lunar)

        // Effective month number used for the Monthly General (月将).
        val effectiveMonth = getEffectiveMonthForForecast(solarTerm)
        logger.debug("is leap month: ${checkLeapMonth(solar.lunar)}")

        val season = solarTerm.toSeason()

        return LunarDate(
            yearStem = HeavenlyStem.fromHanzi(solar.lunar.yearGanExact),
            yearBranch = EarthlyBranch.fromHanzi(solar.lunar.yearZhiExact),
            monthStem = HeavenlyStem.fromHanzi(solar.lunar.monthGanExact),
            monthBranch = EarthlyBranch.fromHanzi(solar.lunar.monthZhiExact),
            dayStem = HeavenlyStem.fromHanzi(solar.lunar.dayGanExact),
            dayBranch = EarthlyBranch.fromHanzi(solar.lunar.dayZhiExact),
            hourStem = HeavenlyStem.fromHanzi(solar.lunar.timeGan),
            hourBranch = EarthlyBranch.fromHanzi(solar.lunar.timeZhi),
            lunarMonth = bazi.lunar.month,
            lunarDay = bazi.lunar.day,
            lunarHour = bazi.lunar.hour,
            isLeapMonth = checkLeapMonth(solar.lunar),
            solarTerm = solarTerm,
            dayIndexInCycle = dayIndexInCycle,
            isLunarNewYear = solar.lunar.day == 1 && solar.lunar.month == 1 && !checkLeapMonth(solar.lunar),
            solarMonth = solar.month,
            solarDay = solar.day,
            solarHour = solar.lunar.hour,
            solarMinute = solar.lunar.minute,
            isLeapYear = isLeapYear,
            effectiveMonth = effectiveMonth,
            monthStemForForecast = monthStemForForecast,
            monthBranchForForecast = monthBranchForForecast,
            monthStemForTaisui = monthStemForTaisui,
            monthBranchForTaisui = monthBranchForTaisui,
            seasonBySolarTerm = season,
            seasonQiElement = season.getBackgroundElement(shanghai)
        )
    }

    /**
     * Converts a local Gregorian date and time into a complete LunarDate.
     *
     * Convenience overload: attaches the given timezone (Asia/Shanghai
     * by default) and delegates to [fromGregorian] with [ZonedDateTime],
     * so the same Zi Hour (子时) day boundary at 23:00 applies.
     *
     * @param dateTime Gregorian date and time.
     * @param zoneId Source timezone. If null, Asia/Shanghai is assumed.
     * @return Complete Chinese calendrical representation.
     * @see fromGregorian
     */
    fun fromGregorian(dateTime: LocalDateTime, zoneId: ZoneId? = null): LunarDate =
        fromGregorian(dateTime.atZone(zoneId ?: SHANGHAI))

    /**
     * Returns the effective seasonal element.
     *
     * During the final eighteen days preceding each seasonal transition,
     * Earth replaces the ordinary seasonal element according to the
     * traditional Chinese calendrical rules.
     */
    private fun FourSeason.getBackgroundElement(shanghaiTime: ZonedDateTime): FiveElement =
        when {
            isEarthDay(shanghaiTime.toLocalDate()) -> FiveElement.EARTH
            else -> element
        }

    /**
     * Lookup table for Solar Terms by their Chinese names.
     */
    private val solarTermChineseMap: Map<String, SolarTerm> =
        SolarTerm.entries.associateBy { it.hanzi }

    /**
     * Lookup table for Solar Terms by enum constant names.
     */
    private val solarTermLatinMap: Map<String, SolarTerm> =
        SolarTerm.entries.associateBy { it.name.uppercase() }

    /**
     * Returns the most recent Solar Term that has already started.
     *
     * The method iterates through the Solar Term table and selects
     * the latest term whose timestamp is not later than the current
     * lunar date and time.
     */
    internal fun getActiveSolarTerm(lunar: Lunar): SolarTerm {

        val now = lunar.solar.toYmdHms()

        var latestTerm: SolarTerm? = null
        var latestTime: String? = null

        for ((rawKey, date) in lunar.jieQiTable) {
            val normalized = rawKey.trim()
            val match = solarTermChineseMap[normalized]
                ?: solarTermLatinMap[normalized.uppercase()]
                ?: continue

            val termTime = date.toYmdHms()

            if (termTime <= now && (latestTime == null || termTime > latestTime)) {
                latestTerm = match
                latestTime = termTime
            }
        }

        return latestTerm  ?: throw IllegalStateException("Failed to determine active solar term for datetime $now")
    }


    /**
     * Returns the effective forecast month determined by the
     * currently active Solar Term.
     *
     * Forecast months follow Solar Term boundaries rather than
     * lunar month boundaries.
     */
    private fun getEffectiveMonthForForecast(lastTerm: SolarTerm): Int {

        return when (lastTerm) {

            SolarTerm.LI_CHUN, SolarTerm.YU_SHUI -> 1

            SolarTerm.JING_ZHE, SolarTerm.CHUN_FEN -> 2

            SolarTerm.QING_MING, SolarTerm.GU_YU -> 3

            SolarTerm.LI_XIA, SolarTerm.XIAO_MAN -> 4

            SolarTerm.MANG_ZHONG, SolarTerm.XIA_ZHI -> 5

            SolarTerm.XIAO_SHU, SolarTerm.DA_SHU -> 6

            SolarTerm.LI_QIU, SolarTerm.CHU_SHU -> 7

            SolarTerm.BAI_LU, SolarTerm.QIU_FEN -> 8

            SolarTerm.HAN_LU, SolarTerm.SHUANG_JIANG -> 9

            SolarTerm.LI_DONG, SolarTerm.XIAO_XUE -> 10

            SolarTerm.DA_XUE, SolarTerm.DONG_ZHI -> 11

            SolarTerm.XIAO_HAN, SolarTerm.DA_HAN -> 12
        }
    }

    /**
     * Determines whether the specified lunar month is a leap month.
     */
    private fun checkLeapMonth(lunar: Lunar): Boolean {

        val trueLeap =
            LunarYear.fromYear(lunar.year)
                .months
                .firstOrNull { it.isLeap && it.year == lunar.year }
                ?: return false

        return lunar.month == trueLeap.month
    }

    /**
     * Returns the effective month pillar used by forecasting.
     *
     * For leap months, the previous regular month's pillar is used
     * according to traditional Chinese calendrical rules.
     */
    private fun getEffectiveMonthGanZhi(lunar: Lunar): Pair<HeavenlyStem, EarthlyBranch> {

        if (!checkLeapMonth(lunar)) {
            return HeavenlyStem.fromHanzi(lunar.monthGan) to
                    EarthlyBranch.fromHanzi(lunar.monthZhi)
        }

        val baseMonth = kotlin.math.abs(lunar.month)
        val prevMonth = (baseMonth - 1).coerceAtLeast(1)

        val prevLunar = Lunar.fromYmdHms(lunar.year, prevMonth, 1, 12, 0, 0)

        return HeavenlyStem.fromHanzi(prevLunar.monthGan) to
                EarthlyBranch.fromHanzi(prevLunar.monthZhi)
    }

    /**
     * Returns the month pillar used for Tai Sui (太岁) calculations.
     *
     * Unlike the ordinary lunar month, the Tai Sui month is determined
     * exclusively by Solar Terms (节气) and is therefore independent of
     * leap months.
     *
     * In the traditional Chinese calendar, the solar year begins
     * at Beginning of Spring (立春). Solar Terms
     * occurring before Li Chun are treated as belonging to the previous
     * solar year.
     */
    internal fun getMonthGanZhiBySolarTerm(
        lunar: Lunar
    ): Pair<HeavenlyStem, EarthlyBranch> =
        HeavenlyStem.fromHanzi(lunar.monthGanExact) to
                EarthlyBranch.fromHanzi(lunar.monthZhiExact)

    /**
     * Maps a Solar Term to its corresponding season.
     *
     * In the traditional Chinese calendar each season
     * consists of six consecutive Solar Terms.
     */
    private fun SolarTerm.toSeason(): FourSeason = when (this) {
        // ——— 春季 ———
        SolarTerm.LI_CHUN,
        SolarTerm.YU_SHUI,
        SolarTerm.JING_ZHE,
        SolarTerm.CHUN_FEN,
        SolarTerm.QING_MING,
        SolarTerm.GU_YU -> FourSeason.SPRING

        // ——— 夏季 ———
        SolarTerm.LI_XIA,
        SolarTerm.XIAO_MAN,
        SolarTerm.MANG_ZHONG,
        SolarTerm.XIA_ZHI,
        SolarTerm.XIAO_SHU,
        SolarTerm.DA_SHU -> FourSeason.SUMMER

        // ——— 秋季 ———
        SolarTerm.LI_QIU,
        SolarTerm.CHU_SHU,
        SolarTerm.BAI_LU,
        SolarTerm.QIU_FEN,
        SolarTerm.HAN_LU,
        SolarTerm.SHUANG_JIANG -> FourSeason.AUTUMN

        // ——— 冬季 ———
        SolarTerm.LI_DONG,
        SolarTerm.XIAO_XUE,
        SolarTerm.DA_XUE,
        SolarTerm.DONG_ZHI,
        SolarTerm.XIAO_HAN,
        SolarTerm.DA_HAN -> FourSeason.WINTER
    }

    /**
     * Returns the lunar month number ignoring leap months.
     *
     * Both month 2 and leap month 2 (闰二月) return 2.
     */
    fun LunarDate.lunarMonthBase(): Int = lunarMonth.absoluteValue
}