package hk.yesdao.calendar.model

/**
 * Five Elements (Wu Xing, 五行).
 *
 * Each element is associated with:
 * - Chinese name (Hanzi)
 * - Pinyin
 * - English name
 * - Traditional color (五色)
 * - Corresponding Zang organ (五脏)
 */
enum class FiveElement(
    val hanzi: String,
    val pinyin: String,
    val english: String,
    val color: FiveColor,
    val organ: Organ
) {

    WOOD(
        hanzi = "木",
        pinyin = "mù",
        english = "Wood",
        color = FiveColor.GREEN,
        organ = Organ.LIVER
    ),

    FIRE(
        hanzi = "火",
        pinyin = "huǒ",
        english = "Fire",
        color = FiveColor.RED,
        organ = Organ.HEART
    ),

    EARTH(
        hanzi = "土",
        pinyin = "tǔ",
        english = "Earth",
        color = FiveColor.YELLOW,
        organ = Organ.SPLEEN
    ),

    METAL(
        hanzi = "金",
        pinyin = "jīn",
        english = "Metal",
        color = FiveColor.WHITE,
        organ = Organ.LUNG
    ),

    WATER(
        hanzi = "水",
        pinyin = "shuǐ",
        english = "Water",
        color = FiveColor.BLACK,
        organ = Organ.KIDNEY
    );

    companion object {

        /**
         * Returns the Five Element matching the specified Chinese character.
         */
        fun fromHanzi(hanzi: String): FiveElement =
            entries.firstOrNull { it.hanzi == hanzi }
                ?: throw IllegalArgumentException("Unknown FiveElement: $hanzi")

        /**
         * Returns the Five Element matching the specified English name.
         */
        fun fromEnglish(name: String): FiveElement =
            entries.firstOrNull { it.english.equals(name, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown FiveElement: $name")
    }
}