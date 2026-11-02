package hk.yesdao.calendar.calendar

import com.nlf.calendar.LunarYear
import hk.yesdao.calendar.converter.LunarDateConverter.fromGregorian
import hk.yesdao.calendar.converter.LunarDateConverter.lunarMonthBase
import hk.yesdao.calendar.model.EarthlyBranch
import hk.yesdao.calendar.model.GanZhi
import hk.yesdao.calendar.model.HeavenlyStem
import hk.yesdao.calendar.model.SolarTerm
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

import java.time.LocalDate
import java.time.LocalDateTime

class ChineseCalendarInvariantTest {

    @Test
    fun `should produce lunar months with 29 or 30 days`() {

        for (year in 1900..2100) {

            val lunarYear = LunarYear.fromYear(year)

            lunarYear.months.forEach {

                assertTrue(
                    it.dayCount == 29 || it.dayCount == 30
                )
            }
        }
    }

    @Test
    fun `should switch to next month after day twenty nine`() {

        val lastDay = fromGregorian(
            LocalDate.of(2023, 5, 18).atTime(12, 0)
        )

        val nextDay = fromGregorian(
            LocalDate.of(2023, 5, 19).atTime(12, 0)
        )

        assertEquals(
            3,
            lastDay.lunarMonthBase()
        )

        assertEquals(
            29,
            lastDay.lunarDay
        )

        assertEquals(
            1,
            nextDay.lunarDay
        )

        assertNotEquals(
            lastDay.lunarMonth,
            nextDay.lunarMonth
        )
    }

    @Test
    fun `should change year pillar exactly at Li Chun`() {

        val beforeLiChun = fromGregorian(
            LocalDateTime.of(2024, 2, 4, 16, 25)
        )

        val afterLiChun = fromGregorian(
            LocalDateTime.of(2024, 2, 4, 16, 27)
        )

        assertEquals(
            "癸卯",
            "${beforeLiChun.yearStem.hanzi}${beforeLiChun.yearBranch.hanzi}"
        )

        assertEquals(
            "甲辰",
            "${afterLiChun.yearStem.hanzi}${afterLiChun.yearBranch.hanzi}"
        )

        assertNotEquals(
            beforeLiChun.yearStem,
            afterLiChun.yearStem
        )

        assertNotEquals(
            beforeLiChun.yearBranch,
            afterLiChun.yearBranch
        )

        assertEquals(
            SolarTerm.LI_CHUN,
            afterLiChun.solarTerm
        )
    }

    @ParameterizedTest(name = "{0} → lunarMonth={2}, lunarDay={3}, leapMonth={1}")
    @DisplayName("Should correctly resolve lunar month boundaries")
    @CsvSource(
        // Lunar New Year transition (2021 → 2022)
        "2022-01-21, false, 12, 19",
        "2022-01-31, false, 12, 29",
        "2022-02-01, false,  1,  1",

        // Regular third lunar month (2023)
        "2023-05-17, false,  3, 28",
        "2023-05-18, false,  3, 29",

        // Leap fourth lunar month (2020)
        "2020-05-21, false,  4, 29",
        "2020-05-22, false,  4, 30",
        "2020-05-23, true,  -4,  1",
        "2020-06-20, true,  -4, 29",
        "2020-06-21, false,  5,  1"
    )
    fun `should resolve lunar month boundaries`(
        date: String,
        expectedLeapMonth: Boolean,
        expectedLunarMonth: Int,
        expectedLunarDay: Int
    ) {

        val lunarDate = fromGregorian(
            LocalDate.parse(date).atTime(12, 0)
        )

        assertEquals(
            expectedLeapMonth,
            lunarDate.isLeapMonth
        )

        assertEquals(
            expectedLunarMonth,
            lunarDate.lunarMonth
        )

        assertEquals(
            expectedLunarDay,
            lunarDate.lunarDay
        )
    }

    @Test
    fun `should switch lunar month at the New Moon`() {

        val before = fromGregorian(
            LocalDateTime.of(2023, 5, 18, 22, 0)
        )

        val after = fromGregorian(
            LocalDateTime.of(2023, 5, 19, 0, 30)
        )

        assertNotEquals(
            before.lunarMonth,
            after.lunarMonth
        )

        assertEquals(
            29,
            before.lunarDay
        )

        assertEquals(
            1,
            after.lunarDay
        )
    }

    @Test
    fun `should produce twelve unique hour pillars`() {

        val base = LocalDateTime.of(2024, 5, 20, 0, 0)

        val hourPillars = mutableSetOf<Pair<HeavenlyStem, EarthlyBranch>>()

        for (hour in 0..22 step 2) {

            val lunar = fromGregorian(
                base.plusHours(hour.toLong())
            )

            hourPillars += lunar.hourStem to lunar.hourBranch
        }

        assertEquals(
            12,
            hourPillars.size
        )
    }

    @Test
    fun `should identify leap lunar month`() {

        val lunarDate = fromGregorian(
            LocalDateTime.of(2023, 4, 19, 12, 0)
        )

        assertTrue(lunarDate.isLeapMonth)

        assertEquals(
            2,
            lunarDate.lunarMonthBase()
        )

        assertTrue(
            lunarDate.dayIndexInCycle in GanZhi.entries.indices
        )
    }

    @Test
    fun `should identify regular lunar month`() {

        val lunarDate = fromGregorian(
            LocalDateTime.of(2023, 3, 12, 12, 0)
        )

        kotlin.test.assertFalse(
            lunarDate.isLeapMonth
        )

        assertEquals(
            2,
            lunarDate.lunarMonth
        )

        assertTrue(
            lunarDate.dayIndexInCycle in GanZhi.entries.indices
        )
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Should not contain leap months in 2021")
    @ValueSource(strings = [
        "2021-01-13",
        "2021-02-12",
        "2021-03-13",
        "2021-04-12",
        "2021-05-12",
        "2021-06-10",
        "2021-07-10",
        "2021-08-08",
        "2021-09-07",
        "2021-10-06",
        "2021-11-05",
        "2021-12-04"
    ])
    fun `should not identify leap month in 2021`(
        date: String
    ) {

        val lunarDate = fromGregorian(
            LocalDate.parse(date).atTime(12, 0)
        )

        kotlin.test.assertFalse(
            lunarDate.isLeapMonth
        )

        assertTrue(
            lunarDate.lunarMonth > 0
        )
    }

    @Test
    fun `should switch into leap lunar month`() {

        val beforeLeap = fromGregorian(
            LocalDateTime.of(2020, 5, 22, 12, 0)
        )

        val leapMonth = fromGregorian(
            LocalDateTime.of(2020, 5, 23, 12, 0)
        )

        kotlin.test.assertFalse(
            beforeLeap.isLeapMonth
        )

        assertTrue(
            leapMonth.isLeapMonth
        )

        assertEquals(
            4,
            leapMonth.lunarMonthBase()
        )
    }

    @Test
    fun `should resolve active Solar Term`() {

        val lunarDate = fromGregorian(
            LocalDateTime.of(2024, 6, 21, 12, 0)
        )

        assertEquals(
            SolarTerm.XIA_ZHI,
            lunarDate.solarTerm
        )
    }
    @Test
    fun `should change month pillar at Li Chun`() {

        val beforeLiChun = fromGregorian(
            LocalDateTime.of(2024, 2, 3, 12, 0)
        )

        val afterLiChun = fromGregorian(
            LocalDateTime.of(2024, 2, 5, 12, 0)
        )

        assertNotEquals(
            beforeLiChun.monthStem,
            afterLiChun.monthStem
        )

        assertNotEquals(
            beforeLiChun.monthBranch,
            afterLiChun.monthBranch
        )

        assertNotEquals(
            beforeLiChun.monthStemForForecast,
            afterLiChun.monthStemForForecast
        )

        assertNotEquals(
            beforeLiChun.monthBranchForForecast,
            afterLiChun.monthBranchForForecast
        )
    }

    @Test
    fun `should resolve lunar date after Li Chun`() {

        val lunarDate = fromGregorian(
            LocalDateTime.of(2022, 2, 4, 12, 0)
        )

        assertEquals(
            SolarTerm.LI_CHUN,
            lunarDate.solarTerm
        )

        assertEquals(
            1,
            lunarDate.lunarMonthBase()
        )

        assertEquals(
            4,
            lunarDate.lunarDay
        )
    }

    @Test
    fun `should identify Jia Zi day`() {

        val lunarDate = fromGregorian(
            LocalDateTime.of(2023, 7, 5, 12, 0)
        )

        assertEquals(
            0,
            lunarDate.dayIndexInCycle
        )

        assertEquals(
            HeavenlyStem.JIA,
            lunarDate.dayStem
        )

        assertEquals(
            EarthlyBranch.ZI,
            lunarDate.dayBranch
        )

        assertEquals(
            GanZhi.JIA_ZI,
            GanZhi.entries[lunarDate.dayIndexInCycle]
        )
    }


    @Test
    fun `should resolve Great Heat Solar Term`() {

        val lunarDate = fromGregorian(
            LocalDateTime.of(2023, 7, 23, 15, 0)
        )

        assertEquals(
            SolarTerm.DA_SHU,
            lunarDate.solarTerm
        )
    }

    @Test
    fun `should keep the same GanZhi day across midnight within Zi hour`() {

        val beforeMidnight = LocalDateTime.of(2024, 5, 20, 23, 59)
        val afterMidnight = LocalDateTime.of(2024, 5, 21, 0, 1)

        val before = fromGregorian(beforeMidnight)
        val after = fromGregorian(afterMidnight)

        // Both moments are in Zi hour (子时), which already belongs to the next day.
        assertEquals(before.dayStem, after.dayStem)
        assertEquals(before.dayBranch, after.dayBranch)
        assertEquals(before.dayIndexInCycle, after.dayIndexInCycle)
        assertEquals(EarthlyBranch.ZI, before.hourBranch)
        assertEquals(EarthlyBranch.ZI, after.hourBranch)
    }

    @Test
    fun `should change GanZhi day at 23_00 when Zi hour begins`() {

        val beforeZi = LocalDateTime.of(2024, 5, 20, 22, 59)
        val atZi = LocalDateTime.of(2024, 5, 20, 23, 0)

        val before = fromGregorian(beforeZi)
        val after = fromGregorian(atZi)

        // The new calendrical day begins at 23:00 (see docs/CALENDAR_RULES.md).
        assertNotEquals(before.dayStem, after.dayStem)
        assertNotEquals(before.dayBranch, after.dayBranch)
        assertEquals((before.dayIndexInCycle + 1) % 60, after.dayIndexInCycle)
    }
}