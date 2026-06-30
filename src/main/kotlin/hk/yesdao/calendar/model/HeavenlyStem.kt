package hk.yesdao.calendar.model

/**
 * Ten Heavenly Stems (天干).
 *
 * Each Heavenly Stem is associated with:
 * - Chinese name (Hanzi)
 * - Pinyin
 * - Position in the 10-stem cycle
 * - Five Element (Wu Xing)
 * - Yin or Yang polarity
 * - Starting Earthly Branch index used for hour stem calculation
 *   according to the traditional Five Rats Escape (五鼠遁) rule.
 */
enum class HeavenlyStem(
    val hanzi: String,
    val pinyin: String,
    val index: Int,
    val element: FiveElement,
    val yinYang: YinYang,

    /**
     * Index of the first Earthly Branch used when calculating
     * the Heavenly Stem of the Zi hour (子时) according to
     * the traditional Five Rats Escape (五鼠遁) rule.
     */
    val hourStartIndex: Int
) {

    JIA("甲", "jiǎ", 0, FiveElement.WOOD, YinYang.YANG, 0),
    YI("乙", "yǐ", 1, FiveElement.WOOD, YinYang.YIN, 2),
    BING("丙", "bǐng", 2, FiveElement.FIRE, YinYang.YANG, 4),
    DING("丁", "dīng", 3, FiveElement.FIRE, YinYang.YIN, 6),
    WU("戊", "wù", 4, FiveElement.EARTH, YinYang.YANG, 8),
    JI("己", "jǐ", 5, FiveElement.EARTH, YinYang.YIN, 0),
    GENG("庚", "gēng", 6, FiveElement.METAL, YinYang.YANG, 2),
    XIN("辛", "xīn", 7, FiveElement.METAL, YinYang.YIN, 4),
    REN("壬", "rén", 8, FiveElement.WATER, YinYang.YANG, 6),
    GUI("癸", "guǐ", 9, FiveElement.WATER, YinYang.YIN, 8);

    companion object {

        private val BY_HANZI = entries.associateBy { it.hanzi }
        private val VALUES = entries.toTypedArray()

        /**
         * Returns the Heavenly Stem matching the specified Chinese character.
         */
        fun fromHanzi(hanzi: String): HeavenlyStem =
            BY_HANZI[hanzi]
                ?: throw IllegalArgumentException("Unknown HeavenlyStem: $hanzi")

        /**
         * Returns the Heavenly Stem by its position
         * within the 10-stem cycle.
         */
        fun fromIndex(index: Int): HeavenlyStem =
            VALUES[index]
    }
}