package hk.yesdao.calendar.model

/**
 * Which clock is used to resolve the day and hour pillars of a birth chart.
 *
 * Year and month pillars always follow the real moment of birth
 * (Solar Terms are astronomical instants); only the day and hour
 * pillars depend on this mode.
 */
enum class BirthTimeMode {

    /** Beijing time (UTC+8), the same as [hk.yesdao.calendar.converter.LunarDateConverter.fromGregorian]. */
    BEIJING,

    /** Local mean solar time (平太阳时): longitude correction only. */
    LOCAL_MEAN_SOLAR,

    /** True solar time (真太阳时): longitude correction plus Equation of Time. */
    TRUE_SOLAR
}
