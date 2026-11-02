package hk.yesdao.calendar.model

/**
 * Five Zang organs (五脏) associated with the Five Elements.
 */
enum class Organ(
    val hanzi: String,
    val english: String
) {
    LIVER("肝", "Liver"),
    HEART("心", "Heart"),
    SPLEEN("脾", "Spleen"),
    LUNG("肺", "Lung"),
    KIDNEY("肾", "Kidney");

    /**
     * Element associated with this organ.
     *
     * Derived from [FiveElement.organ] instead of being stored,
     * to avoid circular enum initialization between the two enums.
     */
    val element: FiveElement
        get() = FiveElement.entries.first { it.organ == this }

    companion object {
        private val BY_HANZI = entries.associateBy { it.hanzi }

        fun fromHanzi(hanzi: String): Organ =
            BY_HANZI[hanzi]
                ?: throw IllegalArgumentException("Unknown Organ: $hanzi")
    }
}