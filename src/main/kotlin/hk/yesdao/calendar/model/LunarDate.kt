package hk.yesdao.calendar.model

/**
 * Represents a complete Chinese calendrical date.
 *
 * A LunarDate combines the traditional Chinese calendar with
 * Gregorian calendar information and additional metadata used by
 * Qi Men Dun Jia (奇门遁甲), BaZi (八字) and other traditional
 * Chinese calendrical systems.
 */
data class LunarDate(

    /** Year Pillar (年柱). */
    val yearStem: HeavenlyStem,
    val yearBranch: EarthlyBranch,

    /** Month Pillar (月柱). */
    val monthStem: HeavenlyStem,
    val monthBranch: EarthlyBranch,

    /** Day Pillar (日柱). */
    val dayStem: HeavenlyStem,
    val dayBranch: EarthlyBranch,

    /** Hour Pillar (时柱). */
    val hourStem: HeavenlyStem,
    val hourBranch: EarthlyBranch,

    /** Lunar calendar date. */
    val lunarMonth: Int,
    val lunarDay: Int,
    val lunarHour: Int,
    val isLeapMonth: Boolean,

    /** Active Solar Term (节气). */
    val solarTerm: SolarTerm?,

    /** Zero-based position within the sexagenary day cycle. */
    val dayIndexInCycle: Int,

    /** True if the date is the first day of the Lunar New Year. */
    val isLunarNewYear: Boolean,

    /** Gregorian calendar date and time. */
    val solarMonth: Int,
    val solarDay: Int,
    val solarHour: Int,
    val solarMinute: Int,
    val isLeapYear: Boolean = false,

    /** Effective month used by forecasting algorithms. */
    val effectiveMonth: Int = 0,

    /** Effective month pillar used for forecasting. */
    val monthStemForForecast: HeavenlyStem,
    val monthBranchForForecast: EarthlyBranch,

    /** Month pillar used for Tai Sui (太岁) calculations. */
    val monthStemForTaisui: HeavenlyStem,
    val monthBranchForTaisui: EarthlyBranch,

    /** Season determined by the current Solar Term. */
    val seasonBySolarTerm: FourSeason = FourSeason.SPRING,

    /** Seasonal Qi element associated with the current Solar Term. */
    val seasonQiElement: FiveElement = FiveElement.WOOD
)