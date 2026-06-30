package hk.yesdao.calendar.model

/**
 * Twenty-Four Solar Terms (二十四节气).
 *
 * Solar Terms divide the traditional Chinese solar year into
 * twenty-four seasonal periods and form the foundation of the
 * Chinese lunisolar calendar.
 *
 * They are widely used in Chinese calendrical calculations,
 * BaZi (八字), Qi Men Dun Jia (奇门遁甲) and Feng Shui (风水).
 */
enum class SolarTerm(
    val hanzi: String,
    val english: String,
    val season: FourSeason
) {

    LI_CHUN("立春", "Beginning of Spring", FourSeason.SPRING),
    YU_SHUI("雨水", "Rain Water", FourSeason.SPRING),
    JING_ZHE("惊蛰", "Awakening of Insects", FourSeason.SPRING),
    CHUN_FEN("春分", "Spring Equinox", FourSeason.SPRING),
    QING_MING("清明", "Clear and Bright", FourSeason.SPRING),
    GU_YU("谷雨", "Grain Rain", FourSeason.SPRING),

    LI_XIA("立夏", "Beginning of Summer", FourSeason.SUMMER),
    XIAO_MAN("小满", "Lesser Fullness", FourSeason.SUMMER),
    MANG_ZHONG("芒种", "Grain in Ear", FourSeason.SUMMER),
    XIA_ZHI("夏至", "Summer Solstice", FourSeason.SUMMER),
    XIAO_SHU("小暑", "Lesser Heat", FourSeason.SUMMER),
    DA_SHU("大暑", "Greater Heat", FourSeason.SUMMER),

    LI_QIU("立秋", "Beginning of Autumn", FourSeason.AUTUMN),
    CHU_SHU("处暑", "Limit of Heat", FourSeason.AUTUMN),
    BAI_LU("白露", "White Dew", FourSeason.AUTUMN),
    QIU_FEN("秋分", "Autumn Equinox", FourSeason.AUTUMN),
    HAN_LU("寒露", "Cold Dew", FourSeason.AUTUMN),
    SHUANG_JIANG("霜降", "Frost Descent", FourSeason.AUTUMN),

    LI_DONG("立冬", "Beginning of Winter", FourSeason.WINTER),
    XIAO_XUE("小雪", "Lesser Snow", FourSeason.WINTER),
    DA_XUE("大雪", "Greater Snow", FourSeason.WINTER),
    DONG_ZHI("冬至", "Winter Solstice", FourSeason.WINTER),
    XIAO_HAN("小寒", "Lesser Cold", FourSeason.WINTER),
    DA_HAN("大寒", "Greater Cold", FourSeason.WINTER);

    companion object {

        private val BY_HANZI = entries.associateBy { it.hanzi }

        /**
         * Returns the Solar Term matching the specified Chinese name.
         */
        fun fromHanzi(hanzi: String): SolarTerm =
            BY_HANZI[hanzi.trim()]
                ?: throw IllegalArgumentException("Unknown SolarTerm: $hanzi")
    }
}