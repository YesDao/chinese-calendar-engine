package hk.yesdao.calendar.time

import hk.yesdao.calendar.time.ShanghaiTimeResolver.SHANGHAI
import hk.yesdao.calendar.time.ShanghaiTimeResolver.ZI_BOUNDARY
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Zi day (子日) of a person at a given place.
 *
 * The calendrical day changes at 23:00. Without a longitude that is 23:00 Asia/Shanghai zone time;
 * with a longitude it is 23:00 of **true solar time** at that longitude (see [SolarTime]).
 *
 * Solar term boundaries are not affected: they stay on Asia/Shanghai
 * (see `SolarTermRangeCache`), only the date that "today" maps to depends on the place.
 */
object ZiDay {

    private const val MAX_ITERATIONS = 5

    /**
     * Zi day containing [instant].
     *
     * @param longitude Geographic longitude in degrees, east positive, −180..180.
     *   `null` means Asia/Shanghai zone time.
     */
    fun of(instant: Instant, longitude: Double? = null): LocalDate =
        if (longitude == null) {
            ShanghaiTimeResolver.resolveZiDate(instant.atZone(SHANGHAI))
        } else {
            ShanghaiTimeResolver.resolveZiDate(SolarTime.trueSolarTime(instant, longitude))
        }

    /** Zi day at the current moment of [clock]. See [of] for [longitude]. */
    fun today(clock: Clock = Clock.systemUTC(), longitude: Double? = null): LocalDate =
        of(clock.instant(), longitude)

    /**
     * First moment of the Zi day that follows the one containing [instant],
     * with whole-second precision: the earliest whole second at which [of] returns the next day.
     *
     * With a [longitude] this is the moment the true solar time reaches 23:00
     * (of the current solar date, or of the next one if it is already past 23:00).
     */
    fun nextStart(instant: Instant, longitude: Double? = null): Instant {
        if (longitude == null) {
            val zdt = instant.atZone(SHANGHAI)
            val date = if (zdt.toLocalTime() >= ZI_BOUNDARY) zdt.toLocalDate().plusDays(1) else zdt.toLocalDate()
            return ZonedDateTime.of(date, ZI_BOUNDARY, SHANGHAI).toInstant()
        }

        val solar = SolarTime.trueSolarTime(instant, longitude)
        val date = if (solar.toLocalTime() >= ZI_BOUNDARY) solar.toLocalDate().plusDays(1) else solar.toLocalDate()
        val target = LocalDateTime.of(date, ZI_BOUNDARY)

        // Solve target = t + longitude·240 s + EoT(t). EoT changes slowly, so the fixed-point iteration converges fast.
        var t = instant
        repeat(MAX_ITERATIONS) {
            val error = Duration.between(SolarTime.trueSolarTime(t, longitude), target)
            if (error.isZero) return@repeat
            t = t.plus(error)
        }
        t = t.truncatedTo(ChronoUnit.SECONDS)

        // Settle on the earliest whole second at or after the boundary.
        while (SolarTime.trueSolarTime(t, longitude) < target) t = t.plusSeconds(1)
        while (SolarTime.trueSolarTime(t.minusSeconds(1), longitude) >= target) t = t.minusSeconds(1)
        return t
    }
}
