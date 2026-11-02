package hk.yesdao.calendar.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FiveElementCorrespondenceTest {

    @Test
    fun `color and organ map back to their element`() {
        // Touch FiveElement first: this order used to leave
        // FiveColor.element and Organ.element as null.
        FiveElement.WOOD

        FiveElement.entries.forEach { element ->
            assertEquals(element, element.color.element)
            assertEquals(element, element.organ.element)
        }
    }
}