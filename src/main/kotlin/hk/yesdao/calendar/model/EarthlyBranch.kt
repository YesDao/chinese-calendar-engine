package hk.yesdao.calendar.model

/**
 * Twelve Earthly Branches (地支).
 *
 * Each branch is associated with:
 * - Chinese character (Hanzi)
 * - Pinyin
 * - Position in the 12-branch cycle
 * - Five Element (Wu Xing)
 * - Yin or Yang polarity
 */
enum class EarthlyBranch(val hanzi: String,
                         val pinyin: String,
                         val index: Int,
                         val element: FiveElement,
                         val yinYang: YinYang) {
    ZI("子", "zǐ", 0, FiveElement.WATER, YinYang.YANG),
    CHOU("丑", "chǒu", 1, FiveElement.EARTH, YinYang.YIN),
    YIN("寅", "yín", 2, FiveElement.WOOD, YinYang.YANG),
    MAO("卯", "mǎo", 3, FiveElement.WOOD, YinYang.YIN),
    CHEN("辰", "chén", 4, FiveElement.EARTH, YinYang.YANG),
    SI("巳", "sì", 5, FiveElement.FIRE, YinYang.YIN),
    WU("午", "wǔ", 6, FiveElement.FIRE, YinYang.YANG),
    WEI("未", "wèi", 7, FiveElement.EARTH, YinYang.YIN),
    SHEN("申", "shēn", 8, FiveElement.METAL, YinYang.YANG),
    YOU("酉", "yǒu", 9, FiveElement.METAL, YinYang.YIN),
    XU("戌", "xū", 10, FiveElement.EARTH, YinYang.YANG),
    HAI("亥", "hài", 11, FiveElement.WATER, YinYang.YIN);

    companion object {

        /**
         * Returns the Earthly Branch matching the specified Chinese character.
         */
        fun fromHanzi(name: String): EarthlyBranch {
            return entries.firstOrNull { it.hanzi.equals(name, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown EarthlyBranch: $name")
        }

        private val VALUES = entries.toTypedArray()

        /**
         * Returns the Earthly Branch by its position in the 12-branch cycle.
         */
        fun fromIndex(index: Int): EarthlyBranch =
            VALUES[index]
    }
}