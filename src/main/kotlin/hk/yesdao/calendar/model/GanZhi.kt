package hk.yesdao.calendar.model

/**
 * Sexagenary cycle (GanZhi, 干支).
 *
 * The sexagenary cycle consists of 60 unique combinations of the
 * Ten Heavenly Stems (天干) and the Twelve Earthly Branches (地支).
 *
 * It is widely used in the Chinese calendar, BaZi (八字),
 * Qi Men Dun Jia (奇门遁甲) and other traditional calendrical systems.
 */
enum class GanZhi(
    val hanzi: String
) {

    JIA_ZI("甲子"),
    YI_CHOU("乙丑"),
    BING_YIN("丙寅"),
    DING_MAO("丁卯"),
    WU_CHEN("戊辰"),
    JI_SI("己巳"),
    GENG_WU("庚午"),
    XIN_WEI("辛未"),
    REN_SHEN("壬申"),
    GUI_YOU("癸酉"),

    JIA_XU("甲戌"),
    YI_HAI("乙亥"),
    BING_ZI("丙子"),
    DING_CHOU("丁丑"),
    WU_YIN("戊寅"),
    JI_MAO("己卯"),
    GENG_CHEN("庚辰"),
    XIN_SI("辛巳"),
    REN_WU("壬午"),
    GUI_WEI("癸未"),

    JIA_SHEN("甲申"),
    YI_YOU("乙酉"),
    BING_XU("丙戌"),
    DING_HAI("丁亥"),
    WU_ZI("戊子"),
    JI_CHOU("己丑"),
    GENG_YIN("庚寅"),
    XIN_MAO("辛卯"),
    REN_CHEN("壬辰"),
    GUI_SI("癸巳"),

    JIA_WU("甲午"),
    YI_WEI("乙未"),
    BING_SHEN("丙申"),
    DING_YOU("丁酉"),
    WU_XU("戊戌"),
    JI_HAI("己亥"),
    GENG_ZI("庚子"),
    XIN_CHOU("辛丑"),
    REN_YIN("壬寅"),
    GUI_MAO("癸卯"),

    JIA_CHEN("甲辰"),
    YI_SI("乙巳"),
    BING_WU("丙午"),
    DING_WEI("丁未"),
    WU_SHEN("戊申"),
    JI_YOU("己酉"),
    GENG_XU("庚戌"),
    XIN_HAI("辛亥"),
    REN_ZI("壬子"),
    GUI_CHOU("癸丑"),

    JIA_YIN("甲寅"),
    YI_MAO("乙卯"),
    BING_CHEN("丙辰"),
    DING_SI("丁巳"),
    WU_WU("戊午"),
    JI_WEI("己未"),
    GENG_SHEN("庚申"),
    XIN_YOU("辛酉"),
    REN_XU("壬戌"),
    GUI_HAI("癸亥");

    companion object {

        private val BY_HANZI =
            entries.associateBy { it.hanzi }

        private val INDEX_BY_HANZI =
            entries.withIndex()
                .associate { it.value.hanzi to it.index }

        /**
         * Returns the GanZhi matching the specified Chinese characters.
         */
        fun fromHanzi(hanzi: String): GanZhi =
            BY_HANZI[hanzi]
                ?: throw IllegalArgumentException("Unknown GanZhi: $hanzi")

        /**
         * Returns true if the specified index is a valid
         * position within the sexagenary cycle.
         */
        fun isValidIndex(index: Int): Boolean =
            index in entries.indices

        /**
         * Returns the zero-based position within the sexagenary cycle.
         *
         * Example:
         * 甲子 -> 0
         * 乙丑 -> 1
         * ...
         * 癸亥 -> 59
         */
        fun indexOf(hanzi: String): Int =
            INDEX_BY_HANZI[hanzi]
                ?: throw IllegalArgumentException("Unknown GanZhi: $hanzi")
    }
}