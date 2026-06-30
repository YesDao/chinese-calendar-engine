package hk.yesdao.calendar.model

/**
 * Five Zang organs (五脏) associated with the Five Elements (Wu Xing).
 *
 * These correspondences are commonly used in traditional
 * Chinese medicine (TCM) and Chinese metaphysics.
 */
enum class Organ(
    val hanzi: String,
    val english: String,
    val element: FiveElement
) {

    LIVER(
        hanzi = "肝",
        english = "Liver",
        element = FiveElement.WOOD
    ),

    HEART(
        hanzi = "心",
        english = "Heart",
        element = FiveElement.FIRE
    ),

    SPLEEN(
        hanzi = "脾",
        english = "Spleen",
        element = FiveElement.EARTH
    ),

    LUNG(
        hanzi = "肺",
        english = "Lung",
        element = FiveElement.METAL
    ),

    KIDNEY(
        hanzi = "肾",
        english = "Kidney",
        element = FiveElement.WATER
    );

    companion object {

        private val BY_HANZI = entries.associateBy { it.hanzi }

        /**
         * Returns the organ matching the specified Chinese character.
         */
        fun fromHanzi(hanzi: String): Organ =
            BY_HANZI[hanzi]
                ?: throw IllegalArgumentException("Unknown Organ: $hanzi")
    }
}