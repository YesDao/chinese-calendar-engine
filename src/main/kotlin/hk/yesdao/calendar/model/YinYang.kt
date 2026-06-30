package hk.yesdao.calendar.model

/**
 * Yin and Yang (阴阳).
 *
 * Yin and Yang represent the two complementary principles that
 * underlie traditional Chinese philosophy, cosmology and medicine.
 *
 * They are used throughout the calendar engine to describe the
 * polarity of Heavenly Stems, Earthly Branches, Five Elements
 * and other traditional concepts.
 */
enum class YinYang(
    val hanzi: String,
    val pinyin: String,
    val english: String
) {

    YANG(
        hanzi = "阳",
        pinyin = "yáng",
        english = "Yang"
    ),

    YIN(
        hanzi = "阴",
        pinyin = "yīn",
        english = "Yin"
    );

    /**
     * Returns the opposite polarity.
     */
    fun opposite(): YinYang =
        when (this) {
            YANG -> YIN
            YIN -> YANG
        }

    companion object {

        private val BY_HANZI =
            entries.associateBy { it.hanzi }

        /**
         * Returns the Yin/Yang polarity matching the specified
         * Chinese character.
         */
        fun fromHanzi(hanzi: String): YinYang =
            BY_HANZI[hanzi]
                ?: throw IllegalArgumentException("Unknown YinYang: $hanzi")
    }
}