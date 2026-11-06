package hk.yesdao.calendar.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GanZhiTest {

    @Test
    fun `fromIndex and from pair are consistent`() {
        for (i in 0 until 60) {
            val gz = GanZhi.fromIndex(i)
            val restored = GanZhi.from(gz.stem, gz.branch)
            assertEquals(gz.index, restored.index)
            assertEquals(gz.hanzi, gz.stem.hanzi + gz.branch.hanzi)
        }
    }

    @Test
    fun `fromIndex wraps around the cycle`() {
        assertEquals(GanZhi.JIA_ZI, GanZhi.fromIndex(60))
        assertEquals(GanZhi.GUI_HAI, GanZhi.fromIndex(-1))
    }

    @Test
    fun `next and prev wrap around the cycle`() {
        assertEquals(GanZhi.JIA_ZI, GanZhi.GUI_HAI.next())
        assertEquals(GanZhi.GUI_HAI, GanZhi.JIA_ZI.prev())
        assertEquals(GanZhi.REN_XU, GanZhi.XIN_YOU.next())
    }

    @Test
    fun `from rejects pairs with different parity`() {
        assertFailsWith<IllegalArgumentException> {
            GanZhi.from(HeavenlyStem.JIA, EarthlyBranch.CHOU)
        }
    }
}
