package hk.yesdao.calendar.converter

import com.nlf.calendar.Lunar
import com.nlf.calendar.LunarYear
import com.nlf.calendar.Solar
import hk.yesdao.calendar.cache.EarthTransitionCache.isEarthDay
import hk.yesdao.calendar.exceprionHandling.IllegalStateException
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
     * Converts a Gregorian date and time into a complete LunarDate.
     *
     * The input date is normalized to the Shanghai timezone, which is
     * the reference timezone used by the traditional Chinese calendar.
     *
     * The resulting LunarDate contains:
     * - Four Pillars (BaZi)
     * - Lunar calendar date
     * - Solar Term
     * - Sexagenary day index
     * - Leap month information
     * - Forecast-specific month data
     * - Seasonal Qi information
     *
     * @param dateTime Gregorian date and time.
     * @param zoneId Source timezone. If null, Asia/Shanghai is assumed.
     * @return Complete Chinese calendrical representation.
     */
    fun fromGregorian(dateTime: LocalDateTime, zoneId: ZoneId? = null): LunarDate {
        logger.debug("Input birth date: {} (zone: {})", dateTime, zoneId)

        val shanghaiTime = dateTime.atZone(zoneId ?: SHANGHAI)

        logger.debug("Zone normalized to Asia/Shanghai: {}", shanghaiTime)

        // Build the lunar calendar representation.
        val solar = Solar.fromDate(Date.from(shanghaiTime.toInstant()))
        val bazi = solar.lunar.eightChar
        logger.debug(
            "Calculated stems/branches: year={}{} month={}{} day={}{} hour={}{}",
            solar.lunar.yearGanExact, solar.lunar.yearZhiExact,
            solar.lunar.monthGanExact, solar.lunar.monthZhiExact,
            solar.lunar.dayGanExact, solar.lunar.dayZhiExact,
            solar.lunar.timeGan, solar.lunar.timeZhi
        )

        // Determine the currently active Solar Term.
        val solarTerm = getActiveSolarTerm(solar.lunar)

        // Determine the zero-based position within the sexagenary day cycle.
        val dayIndexInCycle = GanZhi.indexOf(solar.lunar.dayInGanZhiExact2)

        val isLeapYear = java.time.Year.isLeap(dateTime.year.toLong())

        val (monthStemForForecast, monthBranchForForecast) = getEffectiveMonthGanZhi(solar.lunar)

        val (monthStemForTaisui, monthBranchForTaisui) = getMonthGanZhiBySolarTerm(solar.lunar)

        val effectiveMonth = getEffectiveMonthForForecast(solarTerm)

        val season = solarTerm.toSeason()

        return LunarDate(
            yearStem = HeavenlyStem.fromHanzi(solar.lunar.yearGanExact),
            yearBranch = EarthlyBranch.fromHanzi(solar.lunar.yearZhiExact),
            monthStem = HeavenlyStem.fromHanzi(solar.lunar.monthGanExact),
            monthBranch = EarthlyBranch.fromHanzi(solar.lunar.monthZhiExact),
            dayStem = HeavenlyStem.fromHanzi(solar.lunar.dayGanExact2),
            dayBranch = EarthlyBranch.fromHanzi(solar.lunar.dayZhiExact2),
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
            seasonQiElement = season.getBackgroundElement(shanghaiTime)
        )
    }

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
    ): Pair<HeavenlyStem, EarthlyBranch> {

        val activeTerm =
            getActiveSolarTerm(lunar)

        val termIndex = SolarTerm.entries.indexOf(activeTerm)

        check(termIndex >= 0) {
            "Unknown SolarTerm: $activeTerm"
        }

        // Each solar month consists of two consecutive Solar Terms.
        val solarMonthIndex = termIndex / 2 + 1

        // The solar year starts with Beginning of Spring (立春).
        val solarYear =
            if (activeTerm.ordinal >= SolarTerm.LI_CHUN.ordinal)
                lunar.solar.year
            else
                lunar.solar.year - 1

        // Create a temporary Lunar instance to resolve
        // the month's Heavenly Stem and Earthly Branch.
        val lunarForMonth =
            Lunar.fromYmdHms(
                solarYear,
                solarMonthIndex,
                1,
                12,
                0,
                0
            )

        return HeavenlyStem.fromHanzi(lunarForMonth.monthGan) to
                EarthlyBranch.fromHanzi(lunarForMonth.monthZhi)
    }

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