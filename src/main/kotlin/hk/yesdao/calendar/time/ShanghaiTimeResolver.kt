package hk.yesdao.calendar.time

import java.time.*
import java.time.temporal.ChronoUnit

/**
 * Chinese calendrical time utilities.
 *
 * Implements traditional Zi-hour (子时) rules where
 * the calendrical day changes at 23:00 rather than midnight.
 *
 * Used by lunar calendar, GanZhi and solar term calculations.
 *
 * All calculations are performed relative to Asia/Shanghai,
 * which is the reference timezone used by the Chinese lunar calendar,
 * solar terms (节气) and GanZhi calculations.
 */
object ShanghaiTimeResolver {

    /** Reference timezone used by the calendar engine. */
    val SHANGHAI: ZoneId = ZoneId.of("Asia/Shanghai")

    /**
     * Boundary of Zi hour (子时).
     *
     * Traditional Chinese calendrical systems treat
     * 23:00–00:59 as the first double-hour of the next day.
     */
    val ZI_BOUNDARY: LocalTime = LocalTime.of(23, 0)


    /**
     * Converts local datetime from the provided timezone
     * to the equivalent instant in Asia/Shanghai.
     */
    fun toShanghai(
        localDateTime: LocalDateTime,
        zoneId: ZoneId?
    ): ZonedDateTime =
        localDateTime
            .atZone(zoneId ?: SHANGHAI)
            .withZoneSameInstant(SHANGHAI)

    /**
     * Converts the given instant to Asia/Shanghai timezone.
     */
    fun toShanghai(zonedDateTime: ZonedDateTime): ZonedDateTime =
        zonedDateTime.withZoneSameInstant(SHANGHAI)

    /**
     * Resolves the effective calendar date according to Zi-hour rules.
     *
     * Example:
     * 2025-06-01 22:59 -> 2025-06-01
     * 2025-06-01 23:00 -> 2025-06-02
     */
    fun resolveZiDate(
        localDateTime: LocalDateTime,
        zoneId: ZoneId?
    ): LocalDate {
        val zdt = toShanghai(localDateTime, zoneId)
        return if (zdt.toLocalTime() >= ZI_BOUNDARY)
            zdt.toLocalDate().plusDays(1)
        else
            zdt.toLocalDate()
    }

    /**
     * Resolves the effective calendar date according to Zi-hour rules.
     */
    fun resolveZiDate(zdt: ZonedDateTime): LocalDate =
        if (zdt.toLocalTime() >= ZI_BOUNDARY)
            zdt.toLocalDate().plusDays(1)
        else
            zdt.toLocalDate()


    /**
     * Returns Chinese double-hour index.
     *
     * 0 = Zi (23:00-00:59)
     * 1 = Chou (01:00-02:59)
     * ...
     * 11 = Hai (21:00-22:59)
     */
    fun ziHourIndex(zdt: ZonedDateTime): Int {
        val hour = zdt.hour
        return if (hour >= 23) 0 else (hour + 1) / 2
    }


    /**
     * Generates cache key based on Zi date and Chinese hour.
     *
     * Example:
     * 2025-06-01 23:00 -> 20250602Z0
     */
    fun ziHourKey(zdt: ZonedDateTime): String {
        val ziDate = resolveZiDate(zdt)
        val ziHour = ziHourIndex(zdt)

        return buildString(12) {
            append(ziDate.year)
            append("%02d".format(ziDate.monthValue))
            append("%02d".format(ziDate.dayOfMonth))
            append('Z')
            append(ziHour)
        }
    }

    fun resolveZiDate(dateTime: LocalDateTime): LocalDate =
        if (dateTime.toLocalTime() >= ZI_BOUNDARY)
            dateTime.toLocalDate().plusDays(1)
        else
            dateTime.toLocalDate()


    fun truncatedHour(zdt: ZonedDateTime): LocalDateTime =
        zdt.toLocalDateTime().truncatedTo(ChronoUnit.HOURS)

    /**
     * Returns Chinese double-hour index.
     */
    fun calculateZiHourIndex(dt: LocalDateTime): Int {
        val hour = dt.hour
        return if (hour >= 23) 0 else (hour + 1) / 2
    }

    /**
     * Allocation-free cache key generation for Shanghai local time.
     */
    fun fastZiHourKey(dtShanghai: LocalDateTime): String {
        val ziDate = resolveZiDate(dtShanghai)
        val ziHour = calculateZiHourIndex(dtShanghai)

        return buildString(12) {
            append(ziDate.year)
            append("%02d".format(ziDate.monthValue))
            append("%02d".format(ziDate.dayOfMonth))
            append('Z')
            append(ziHour)
        }
    }


    fun toShanghai(
        instant: Instant
    ): ZonedDateTime =
        instant.atZone(SHANGHAI)

    fun now(clock: Clock = Clock.systemUTC()): ZonedDateTime =
        ZonedDateTime.now(clock).withZoneSameInstant(SHANGHAI)

    fun today(clock: Clock = Clock.systemUTC()): LocalDate =
        now(clock).toLocalDate()


    fun toShanghaiLocalDateTime(
        instant: Instant
    ): LocalDateTime =
        toShanghai(instant).toLocalDateTime()

}



