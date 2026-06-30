package hk.yesdao.calendar.cache

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class EarthTransitionCacheTest {

    @Test
    fun `should mark first day of transition period as earth day`() {
        val jieQi = JieQiCache.getForYear(2025)
        val lichun = jieQi["立春"]!!

        val firstEarthDay = LocalDate.of(
            lichun.year,
            lichun.month,
            lichun.day
        ).minusDays(18)

        assertTrue(
            EarthTransitionCache.isEarthDay(firstEarthDay)
        )
    }

    @Test
    fun `should mark last day before solar term as earth day`() {
        val jieQi = JieQiCache.getForYear(2025)
        val lichun = jieQi["立春"]!!

        val lastEarthDay = LocalDate.of(
            lichun.year,
            lichun.month,
            lichun.day
        ).minusDays(1)

        assertTrue(
            EarthTransitionCache.isEarthDay(lastEarthDay)
        )
    }

    @Test
    fun `should not mark day before transition period`() {
        val jieQi = JieQiCache.getForYear(2025)
        val lichun = jieQi["立春"]!!

        val dayBefore = LocalDate.of(
            lichun.year,
            lichun.month,
            lichun.day
        ).minusDays(19)

        assertFalse(
            EarthTransitionCache.isEarthDay(dayBefore)
        )
    }

    @Test
    fun `should not include solar term day`() {
        val jieQi = JieQiCache.getForYear(2025)
        val lichun = jieQi["立春"]!!

        val solarTermDay = LocalDate.of(
            lichun.year,
            lichun.month,
            lichun.day
        )

        assertFalse(
            EarthTransitionCache.isEarthDay(solarTermDay)
        )
    }

    @Test
    fun `should detect earth day for all four seasonal transitions`() {

        val terms = listOf(
            "立春",
            "立夏",
            "立秋",
            "立冬"
        )

        val jieQi = JieQiCache.getForYear(2025)

        terms.forEach { term ->

            val solar = jieQi[term]!!

            val earthDay = LocalDate.of(
                solar.year,
                solar.month,
                solar.day
            ).minusDays(10)

            assertTrue(
                EarthTransitionCache.isEarthDay(earthDay),
                "$term should contain Earth days"
            )
        }
    }
}