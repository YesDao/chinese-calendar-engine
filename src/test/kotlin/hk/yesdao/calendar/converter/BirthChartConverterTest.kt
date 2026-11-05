package hk.yesdao.calendar.converter

import hk.yesdao.calendar.converter.LunarDateConverter.fromBirth
import hk.yesdao.calendar.converter.LunarDateConverter.fromGregorian
import hk.yesdao.calendar.model.BirthTimeMode
import hk.yesdao.calendar.model.EarthlyBranch
import hk.yesdao.calendar.model.GanZhi
import hk.yesdao.calendar.model.HeavenlyStem
import hk.yesdao.calendar.model.LunarDate
import hk.yesdao.calendar.time.SolarTime
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BirthChartConverterTest {

    private val shanghai = ZoneId.of("Asia/Shanghai")

    private fun at(zone: String, y: Int, m: Int, d: Int, h: Int, min: Int): ZonedDateTime =
        LocalDateTime.of(y, m, d, h, min).atZone(ZoneId.of(zone))

    /** Day pillar of the given civil date, taken at noon (no Zi-hour ambiguity). */
    private fun dayOf(y: Int, m: Int, d: Int): LunarDate =
        fromGregorian(LocalDateTime.of(y, m, d, 12, 0), shanghai)

    private fun assertSameDay(expected: LunarDate, actual: LunarDate) {
        assertEquals(expected.dayStem, actual.dayStem, "day stem")
        assertEquals(expected.dayBranch, actual.dayBranch, "day branch")
        assertEquals(expected.dayIndexInCycle, actual.dayIndexInCycle, "dayIndexInCycle")
    }

    private fun assertSameYearAndMonth(expected: LunarDate, actual: LunarDate) {
        assertEquals(expected.yearStem, actual.yearStem)
        assertEquals(expected.yearBranch, actual.yearBranch)
        assertEquals(expected.monthStem, actual.monthStem)
        assertEquals(expected.monthBranch, actual.monthBranch)
        assertEquals(expected.solarTerm, actual.solarTerm)
    }

    private fun assertHourStemFollowsDay(date: LunarDate) =
        assertEquals(
            HeavenlyStem.calculateHourHeavenlyStemFromDay(date.dayStem, date.hourBranch),
            date.hourStem,
            "hour stem must follow day stem (五鼠遁)"
        )

    // --- SolarTime -------------------------------------------------------

    @Test
    fun `equation of time matches known yearly extremes and zeros`() {
        fun eot(iso: String) = SolarTime.equationOfTime(Instant.parse(iso)).toMillis() / 60_000.0

        assertTrue(eot("2026-11-03T04:00:00Z") in 16.0..16.8, "early November ≈ +16.4 min")
        assertTrue(eot("2026-02-11T04:00:00Z") in -14.6..-13.8, "mid February ≈ −14.2 min")
        assertTrue(eot("2026-07-26T04:00:00Z") in -7.0..-6.0, "late July ≈ −6.5 min")
        assertTrue(kotlin.math.abs(eot("2026-04-15T04:00:00Z")) < 1.0, "mid April ≈ 0")
    }

    @Test
    fun `mean solar time at 120 degrees east equals Beijing time`() {
        val birth = at("Asia/Shanghai", 1995, 3, 7, 6, 15)
        assertEquals(
            birth.toLocalDateTime(),
            SolarTime.localMeanSolarTime(birth.toInstant(), 120.0)
        )
    }

    @Test
    fun `longitude outside range is rejected`() {
        val birth = at("Asia/Shanghai", 1995, 3, 7, 6, 15)
        assertFailsWith<IllegalArgumentException> { fromBirth(birth, 181.0) }
        assertFailsWith<IllegalArgumentException> { fromBirth(birth, Double.NaN) }
    }

    // --- fromBirth -------------------------------------------------------

    @Test
    fun `BEIJING mode is identical to fromGregorian`() {
        val birth = at("Europe/Berlin", 1990, 6, 15, 23, 20)
        assertEquals(fromGregorian(birth), fromBirth(birth, 13.4, BirthTimeMode.BEIJING))
    }

    @Test
    fun `true solar time after 23-00 gives Zi hour and day pillar of the next day`() {
        // Shanghai 121.47° E: 22:45 Beijing time ≈ 23:07 true solar time.
        val birth = at("Asia/Shanghai", 2026, 11, 3, 22, 45)

        val beijing = fromGregorian(birth)
        assertEquals(EarthlyBranch.HAI, beijing.hourBranch)
        assertSameDay(dayOf(2026, 11, 3), beijing)

        val chart = fromBirth(birth, 121.47)
        assertEquals(EarthlyBranch.ZI, chart.hourBranch)
        assertSameDay(dayOf(2026, 11, 4), chart)
        assertHourStemFollowsDay(chart)
        assertSameYearAndMonth(beijing, chart)
    }

    @Test
    fun `west of China solar time moves the chart back to the previous day`() {
        // Kashgar 75.99° E, clocks on Beijing time: 01:30 ≈ 22:27 true solar time of the previous day.
        val birth = at("Asia/Shanghai", 2000, 1, 10, 1, 30)

        val beijing = fromGregorian(birth)
        assertEquals(EarthlyBranch.CHOU, beijing.hourBranch)
        assertSameDay(dayOf(2000, 1, 10), beijing)

        val chart = fromBirth(birth, 75.99)
        assertEquals(EarthlyBranch.HAI, chart.hourBranch)
        assertSameDay(dayOf(2000, 1, 9), chart)
        assertHourStemFollowsDay(chart)
        assertSameYearAndMonth(beijing, chart)
    }

    @Test
    fun `birth in Europe uses local Sun instead of Beijing clock`() {
        // Berlin 13.4° E, 23:20 CEST = 21:20 UTC ≈ 22:13 true solar time.
        val birth = at("Europe/Berlin", 1990, 6, 15, 23, 20)

        val beijing = fromBirth(birth, 13.4, BirthTimeMode.BEIJING) // 05:20 on 16 June
        assertEquals(EarthlyBranch.MAO, beijing.hourBranch)
        assertSameDay(dayOf(1990, 6, 16), beijing)

        val chart = fromBirth(birth, 13.4)
        assertEquals(EarthlyBranch.HAI, chart.hourBranch)
        assertSameDay(dayOf(1990, 6, 15), chart)
        assertHourStemFollowsDay(chart)

        val mean = fromBirth(birth, 13.4, BirthTimeMode.LOCAL_MEAN_SOLAR)
        assertEquals(EarthlyBranch.HAI, mean.hourBranch)
    }

    @Test
    fun `hourBranchShift moves by double hours and carries the day across Zi`() {
        val birth = at("Europe/Berlin", 1990, 6, 15, 23, 20) // ≈ 22:13 true solar, 亥

        val earlier = fromBirth(birth, 13.4, hourBranchShift = -1)
        assertEquals(EarthlyBranch.XU, earlier.hourBranch)
        assertSameDay(dayOf(1990, 6, 15), earlier)
        assertHourStemFollowsDay(earlier)

        val later = fromBirth(birth, 13.4, hourBranchShift = 1) // ≈ 00:13 → 子 of 16 June
        assertEquals(EarthlyBranch.ZI, later.hourBranch)
        assertSameDay(dayOf(1990, 6, 16), later)
        assertHourStemFollowsDay(later)
    }

    @Test
    fun `clock fields follow the effective birth time`() {
        // Banská Bystrica 19.149° E, 06:45 CET ≈ 07:18 true solar; shift +1 → ≈ 09:18 (巳).
        val birth = at("Europe/Bratislava", 1983, 10, 30, 6, 45)
        val chart = fromBirth(birth, 19.14932, hourBranchShift = 1)

        assertEquals(EarthlyBranch.SI, chart.hourBranch)
        assertEquals(9, chart.solarHour)
        assertEquals(9, chart.lunarHour)
        assertEquals(10, chart.solarMonth)
        assertEquals(30, chart.solarDay)

        // Same hour pillar as 03:45 on the Beijing clock (10:45 in Shanghai).
        val beijing = fromBirth(at("Europe/Bratislava", 1983, 10, 30, 3, 45), 19.14932, BirthTimeMode.BEIJING)
        assertEquals(beijing.hourStem to beijing.hourBranch, chart.hourStem to chart.hourBranch)
        assertEquals(10, beijing.lunarHour)
    }

    @Test
    fun `clock date moves with the solar clock across midnight`() {
        // Kashgar 75.99° E: 01:30 Beijing ≈ 22:27 true solar on the previous day.
        val chart = fromBirth(at("Asia/Shanghai", 2000, 1, 10, 1, 30), 75.99)
        assertEquals(1, chart.solarMonth)
        assertEquals(9, chart.solarDay)
        assertEquals(22, chart.solarHour)
        assertEquals(dayOf(2000, 1, 9).lunarDay, chart.lunarDay)
    }

    @Test
    fun `year pillar follows the real instant of Li Chun, not solar time`() {
        // 立春 2024: 2024-02-04 16:27 Beijing time. Kashgar solar time is ~3 h behind.
        val before = fromBirth(at("Asia/Shanghai", 2024, 2, 4, 16, 20), 75.99)
        val after = fromBirth(at("Asia/Shanghai", 2024, 2, 4, 16, 35), 75.99)

        assertEquals(HeavenlyStem.GUI to EarthlyBranch.MAO, before.yearStem to before.yearBranch)
        assertEquals(HeavenlyStem.JIA to EarthlyBranch.CHEN, after.yearStem to after.yearBranch)
        assertEquals(EarthlyBranch.CHOU, before.monthBranch)
        assertEquals(EarthlyBranch.YIN, after.monthBranch)
    }

    @Test
    fun `pillars stay consistent for random births worldwide`() {
        val random = Random(42)
        val from = Instant.parse("1930-01-01T00:00:00Z").epochSecond
        val to = Instant.parse("2040-01-01T00:00:00Z").epochSecond

        repeat(3000) {
            val birth = Instant.ofEpochSecond(random.nextLong(from, to)).atZone(shanghai)
            val longitude = random.nextDouble(-180.0, 180.0)
            val mode = BirthTimeMode.entries[random.nextInt(BirthTimeMode.entries.size)]

            val chart = fromBirth(birth, longitude, mode)

            assertHourStemFollowsDay(chart)
            assertEquals(
                GanZhi.indexOf(chart.dayStem.hanzi + chart.dayBranch.hanzi),
                chart.dayIndexInCycle,
                "dayIndexInCycle for $birth @ $longitude"
            )
            assertSameYearAndMonth(fromGregorian(birth), chart)
        }
    }
}
