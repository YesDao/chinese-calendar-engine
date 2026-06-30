package hk.yesdao.calendar.model

/**
 * Five colors (五色) associated with the Five Elements (Wu Xing).
 */
enum class FiveColor(
    val hanzi: String,
    val element: FiveElement
) {
    BLACK("黑", FiveElement.WATER),
    RED("赤", FiveElement.FIRE),
    GREEN("青", FiveElement.WOOD),
    WHITE("白", FiveElement.METAL),
    YELLOW("黄", FiveElement.EARTH)
}