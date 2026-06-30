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

    @Test
    fun `should use previous month pillar for forecast in leap month`() {

        val lunarDate = fromGregorian(
            LocalDateTime.of(2023, 4, 8, 12, 0),
            shanghaiZoneId
        )

        assertTrue(
            lunarDate.isLeapMonth
        )

        assertNotEquals(
            lunarDate.monthStem,
            lunarDate.monthStemForForecast
        )

        assertNotEquals(
            lunarDate.monthBranch,
            lunarDate.monthBranchForForecast
        )
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