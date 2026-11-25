package hk.yesdao.calendar.converter

import com.nlf.calendar.Solar
import hk.yesdao.calendar.converter.LunarDateConverter.fromGregorian
import hk.yesdao.calendar.converter.LunarDateConverter.getActiveSolarTerm
import hk.yesdao.calendar.converter.LunarDateConverter.lunarMonthBase
import hk.yesdao.calendar.model.EarthlyBranch
import hk.yesdao.calendar.model.GanZhi
import hk.yesdao.calendar.model.HeavenlyStem
import hk.yesdao.calendar.model.SolarTerm
import hk.yesdao.calendar.time.ShanghaiTimeResolver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.math.absoluteValue
import kotlin.test.assertFalse

class LunarDateConverterTest {

    private val shanghaiZoneId = ZoneId.of("Asia/Shanghai")

    @ParameterizedTest
    @CsvSource(
        "2023-04-08, 丙辰",
        "2023-03-25, 乙卯",
        "2025-08-01, 癸未",
        "2025-08-10, 甲申",
        "2020-06-10, 壬午"
    )
    fun `forecast month pillar follows solar terms in leap months`(date: String, expected: String) {
        val d = LocalDate.parse(date)
        val lunarDate = fromGregorian(d.atTime(12, 0), shanghaiZoneId)

        assertTrue(lunarDate.isLeapMonth)
        assertEquals(
            expected,
            lunarDate.monthStemForForecast.hanzi + lunarDate.monthBranchForForecast.hanzi
        )
    }

    @Test
    fun `forecast month equals Tai Sui month on every non-jie day 1984-2044`() {
        var day = LocalDate.of(1984, 1, 1)
        val end = LocalDate.of(2044, 12, 31)
        var checked = 0
        while (!day.isAfter(end)) {
            val isJieDay = Solar.fromYmd(day.year, day.monthValue, day.dayOfMonth).lunar.jie.isNotEmpty()
            if (!isJieDay) {
                val d = fromGregorian(day.atTime(12, 0), shanghaiZoneId)
                assertEquals(d.monthStemForTaisui, d.monthStemForForecast, "stem on $day")
                assertEquals(d.monthBranchForTaisui, d.monthBranchForForecast, "branch on $day")
                checked++
            }
            day = day.plusDays(1)
        }
        println("Forecast/TaiSui invariant: $checked days checked")
    }

    @Test
    fun `should use current month pillar for forecast in regular month`() {

        val lunarDate = fromGregorian(
            LocalDateTime.of(2023, 5, 10, 12, 0),
            shanghaiZoneId
        )

        assertFalse(
            lunarDate.isLeapMonth
        )

        assertEquals(
            lunarDate.monthStem,
            lunarDate.monthStemForForecast
        )

        assertEquals(
            lunarDate.monthBranch,
            lunarDate.monthBranchForForecast
        )
    }

    @Test
    fun `should convert regular Gregorian date`() {

        val result = fromGregorian(
            LocalDateTime.of(2025, 5, 19, 22, 22)
        )

        assertFalse(result.isLeapMonth)

        assertTrue(GanZhi.isValidIndex(result.dayIndexInCycle))
    }

    @Test
    fun `should identify Lunar New Year`() {

        val result = fromGregorian(
            LocalDateTime.of(2024, 2, 10, 8, 0)
        )

        assertTrue(result.isLunarNewYear)

        assertEquals(1, result.lunarDay)

        assertFalse(result.isLeapMonth)

        assertTrue(GanZhi.isValidIndex(result.dayIndexInCycle))
    }

    @Test
    fun `should return canonical month number for leap month`() {

        val lunarDate = fromGregorian(
            LocalDateTime.of(2023, 4, 19, 12, 0)
        )

        assertEquals(
            lunarDate.lunarMonth.absoluteValue,
            lunarDate.lunarMonthBase()
        )
    }

    @Test
    fun `should resolve active Solar Term before Li Chun`() {

        val lunar = Solar.fromDate(
            Date.from(
                LocalDateTime.of(2024, 2, 4, 15, 0)
                    .atZone(shanghaiZoneId)
                    .toInstant()
            )
        ).lunar

        assertEquals(
            SolarTerm.DA_HAN,
            getActiveSolarTerm(lunar)
        )
    }

    @Test
    fun `should produce identical lunar date for the same instant`() {

        val instant = Instant.parse("2024-05-20T00:00:00Z")

        val shanghai = fromGregorian(
            instant.atZone(ZoneId.of("Asia/Shanghai")).toLocalDateTime(),
            ZoneId.of("Asia/Shanghai")
        )

        val tokyo = fromGregorian(
            instant.atZone(ZoneId.of("Asia/Tokyo")).toLocalDateTime(),
            ZoneId.of("Asia/Tokyo")
        )

        assertEquals(shanghai, tokyo)
    }

    @Test
    fun `should respect source timezone`() {

        val shanghai = fromGregorian(
            LocalDateTime.of(2024, 5, 20, 7, 30),
            ZoneId.of("Asia/Shanghai")
        )

        val utc = fromGregorian(
            LocalDateTime.of(2024, 5, 20, 7, 30),
            ZoneId.of("UTC")
        )

        assertNotEquals(
            shanghai.hourBranch,
            utc.hourBranch
        )
    }

    @Test
    fun `should normalize calculations to Shanghai timezone`() {

        val localDateTime = LocalDateTime.of(
            2024,
            5,
            20,
            12,
            0
        )

        val shanghai = ShanghaiTimeResolver.toShanghai(
            localDateTime,
            ZoneId.of("Asia/Shanghai")
        )

        val utc = ShanghaiTimeResolver.toShanghai(
            localDateTime,
            ZoneId.of("UTC")
        )

        assertEquals(
            ZoneId.of("Asia/Shanghai"),
            shanghai.zone
        )

        assertEquals(
            ZoneId.of("Asia/Shanghai"),
            utc.zone
        )
    }
}