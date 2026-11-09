package hk.yesdao.calendar.time

import hk.yesdao.calendar.cache.SolarTermRangeCache
import hk.yesdao.calendar.time.ShanghaiTimeResolver.SHANGHAI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ZiDayTest {

    private val hcmc = 106.70
    private val moscow = 37.62
    private val losAngeles = -118.24

    @Test
    fun `null longitude matches Shanghai zone rule`() {
        listOf(
            Instant.parse("2026-11-09T14:59:00Z"), // 22:59 Shanghai
            Instant.parse("2026-11-09T15:00:00Z"), // 23:00 Shanghai
            Instant.parse("2026-11-09T16:30:00Z")
        ).forEach {
            assertEquals(ShanghaiTimeResolver.resolveZiDate(it.atZone(SHANGHAI)), ZiDay.of(it))
        }
        assertEquals(LocalDate.of(2026, 11, 9), ZiDay.of(Instant.parse("2026-11-09T14:59:00Z")))
        assertEquals(LocalDate.of(2026, 11, 10), ZiDay.of(Instant.parse("2026-11-09T15:00:00Z")))
        assertEquals(Instant.parse("2026-11-09T15:00:00Z"), ZiDay.nextStart(Instant.parse("2026-11-09T03:00:00Z")))
        assertEquals(Instant.parse("2026-11-10T15:00:00Z"), ZiDay.nextStart(Instant.parse("2026-11-09T15:00:00Z")))
    }

    @Test
    fun `today uses the clock`() {
        val clock = Clock.fixed(Instant.parse("2026-11-09T15:00:00Z"), ZoneOffset.UTC)
        assertEquals(LocalDate.of(2026, 11, 10), ZiDay.today(clock))
        assertEquals(LocalDate.of(2026, 11, 9), ZiDay.today(clock, hcmc))
    }

    @Test
    fun `Ho Chi Minh City changes day near 15-37 UTC, not 15-00`() {
        val start = ZiDay.nextStart(Instant.parse("2026-11-09T05:00:00Z"), hcmc)
        // 22:37 local mean + ~16 min EoT in early November => ≈ 15:37 UTC
        assertTrue(Duration.between(Instant.parse("2026-11-09T15:37:00Z"), start).abs() < Duration.ofMinutes(2), "$start")
        assertEquals(LocalDate.of(2026, 11, 9), ZiDay.of(start.minusSeconds(60), hcmc))
        assertEquals(LocalDate.of(2026, 11, 10), ZiDay.of(start.plusSeconds(60), hcmc))
        assertEquals(LocalDate.of(2026, 11, 9), ZiDay.of(Instant.parse("2026-11-09T15:00:00Z"), hcmc))
    }

    @Test
    fun `Moscow changes day near 20-13 UTC in November`() {
        val start = ZiDay.nextStart(Instant.parse("2026-11-09T05:00:00Z"), moscow)
        assertTrue(Duration.between(Instant.parse("2026-11-09T20:13:00Z"), start).abs() < Duration.ofMinutes(2), "$start")
        assertEquals(LocalDate.of(2026, 11, 9), ZiDay.of(start.minusSeconds(60), moscow))
        assertEquals(LocalDate.of(2026, 11, 10), ZiDay.of(start.plusSeconds(60), moscow))
    }

    @Test
    fun `Los Angeles is a day behind Shanghai once Shanghai passes 23-00`() {
        // 12:00Z is 20:00 in Shanghai and ≈ 04:23 solar in LA: the same Zi day.
        val noon = Instant.parse("2026-11-09T12:00:00Z")
        assertEquals(ZiDay.of(noon), ZiDay.of(noon, losAngeles))
        // 16:00Z is 00:00 next day in Shanghai (Zi day 10 Nov), ≈ 08:23 solar in LA (9 Nov).
        val t = Instant.parse("2026-11-09T16:00:00Z")
        assertEquals(LocalDate.of(2026, 11, 10), ZiDay.of(t))
        assertEquals(LocalDate.of(2026, 11, 9), ZiDay.of(t, losAngeles))
    }

    @Test
    fun `date line longitudes`() {
        for (lon in listOf(179.9, -179.9)) {
            val t = Instant.parse("2026-11-09T12:00:00Z")
            val start = ZiDay.nextStart(t, lon)
            assertEquals(ZiDay.of(t, lon).plusDays(1), ZiDay.of(start, lon))
            assertEquals(ZiDay.of(t, lon), ZiDay.of(start.minusSeconds(1), lon))
        }
        // 179.9 is almost a day ahead of -179.9
        val t = Instant.parse("2026-11-09T12:00:00Z")
        assertEquals(ZiDay.of(t, -179.9).plusDays(1), ZiDay.of(t, 179.9))
    }

    @Test
    fun `boundary shifts by the equation of time`() {
        fun shiftMinutes(day: String): Double {
            val t = Instant.parse("${day}T00:00:00Z")
            val start = ZiDay.nextStart(t, 0.0)
            // At longitude 0 mean solar time is UTC, so the boundary is 23:00 UTC minus EoT.
            val mean = start.atOffset(ZoneOffset.UTC)
            return (mean.hour * 60 + mean.minute + mean.second / 60.0) - 23 * 60
        }
        val feb = shiftMinutes("2026-02-12")
        val nov = shiftMinutes("2026-11-03")
        assertTrue(feb in 12.0..16.0, "feb=$feb")   // EoT ≈ −14 min => boundary later
        assertTrue(nov in -18.0..-14.0, "nov=$nov") // EoT ≈ +16 min => boundary earlier
    }

    @Test
    fun `nextStart is exact boundary for many instants and longitudes`() {
        val longitudes = listOf(-179.9, -118.24, -45.0, 0.0, 13.4, 37.62, 106.7, 121.47, 179.9, 180.0, -180.0)
        var t = Instant.parse("2026-01-01T00:07:13Z")
        while (t < Instant.parse("2027-01-01T00:00:00Z")) {
            for (lon in longitudes) {
                val day = ZiDay.of(t, lon)
                val start = ZiDay.nextStart(t, lon)
                assertTrue(start > t, "start=$start t=$t lon=$lon")
                assertEquals(day.plusDays(1), ZiDay.of(start, lon), "t=$t lon=$lon")
                assertEquals(day, ZiDay.of(start.minusSeconds(1), lon), "t=$t lon=$lon")
            }
            val shanghaiDay = ZiDay.of(t)
            val shanghaiStart = ZiDay.nextStart(t)
            assertEquals(shanghaiDay.plusDays(1), ZiDay.of(shanghaiStart))
            assertEquals(shanghaiDay, ZiDay.of(shanghaiStart.minusSeconds(1)))
            t = t.plus(Duration.ofHours(7).plusMinutes(41).plusSeconds(17))
        }
    }

    @Test
    fun `invalid longitude is rejected`() {
        val t = Instant.parse("2026-11-09T00:00:00Z")
        listOf(180.1, -180.1, Double.NaN, Double.POSITIVE_INFINITY).forEach {
            assertThrows(IllegalArgumentException::class.java) { ZiDay.of(t, it) }
            assertThrows(IllegalArgumentException::class.java) { ZiDay.nextStart(t, it) }
        }
    }

    @Test
    fun `resolveActiveSolarTerm by longitude on the day a term changes`() {
        // Find a term whose transition Zi day D has Shanghai rule; at the first moment of D in Shanghai
        // Los Angeles is still on the previous day, hence on the previous term.
        val range = SolarTermRangeCache.getForYear(2026)[10]
        val firstInstant = range.ziStart.minusDays(1).atTime(23, 0).atZone(SHANGHAI).toInstant()
        val shanghai = SolarTermRangeCache.resolveActiveSolarTerm(firstInstant, null)
        val la = SolarTermRangeCache.resolveActiveSolarTerm(firstInstant, losAngeles)
        assertEquals(range.term, shanghai.term)
        assertEquals(range.absoluteIndex - 1, la.absoluteIndex)
        assertEquals(SolarTermRangeCache.resolveActiveSolarTerm(firstInstant), shanghai)
    }
}
