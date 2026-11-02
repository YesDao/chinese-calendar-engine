package hk.yesdao.calendar.model

/**
 * Five Colors (五色) associated with the Five Elements.
 */
enum class FiveColor(
    val hanzi: String
) {
    BLACK("黑"),
    RED("赤"),
    GREEN("青"),
    WHITE("白"),
    YELLOW("黄");

    /**
     * Element associated with this color.
     *
     * Derived from [FiveElement.color] instead of being stored,
     * to avoid circular enum initialization between the two enums.
     */
    val element: FiveElement
        get() = FiveElement.entries.first { it.color == this }
}