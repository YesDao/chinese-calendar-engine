package hk.yesdao.calendar.model

/**
 * Four Seasons (四季) used by the calendar engine.
 *
 * Each season is associated with:
 * - Chinese name (Hanzi)
 * - Corresponding Five Element (Wu Xing)
 */
enum class FourSeason(
    val hanzi: String,
    val english: String,
    val element: FiveElement
) {

    SPRING(
        hanzi = "春",
        english = "Spring",
        element = FiveElement.WOOD
    ),

    SUMMER(
        hanzi = "夏",
        english = "Summer",
        element = FiveElement.FIRE
    ),

    AUTUMN(
        hanzi = "秋",
        english = "Autumn",
        element = FiveElement.METAL
    ),

    WINTER(
        hanzi = "冬",
        english = "Winter",
        element = FiveElement.WATER
    );

    companion object {

        /**
         * Returns the season matching the specified Chinese character.
         */
        fun fromHanzi(hanzi: String): FourSeason =
            entries.firstOrNull { it.hanzi == hanzi }
                ?: throw IllegalArgumentException("Unknown FourSeason: $hanzi")
    }
}