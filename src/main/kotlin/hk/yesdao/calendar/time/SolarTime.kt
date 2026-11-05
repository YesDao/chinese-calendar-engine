package hk.yesdao.calendar.time

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.Year
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin

/**
 * Local solar time (地方时 / 真太阳时).
 *
 * Civil time zones are political: the whole of China uses UTC+8, and
 * a person born in Europe has their birth time recorded in a zone
 * that may differ from the Sun's position by an hour or more.
 * The hour pillar (时柱) of a birth chart follows the Sun at the
 * place of birth, so for birth charts the instant has to be
 * expressed in local solar time.
 *
 * - **Local mean solar time** (平太阳时): UTC shifted by longitude,
 *   4 minutes per degree (east positive).
 * - **True (apparent) solar time** (真太阳时): mean solar time plus
 *   the Equation of Time, which varies over the year between about
 *   −14 and +16 minutes.
 *
 * The returned values are wall-clock readings of a sundial at the
 * given longitude; they carry no timezone.
 */
object SolarTime {

    private const val SECONDS_PER_DEGREE = 240.0 // 24 h / 360°

    /**
     * Local mean solar time at the given longitude.
     *
     * @param instant Moment in time (e.g. the moment of birth).
     * @param longitude Geographic longitude in degrees, east positive, −180..180.
     */
    fun localMeanSolarTime(instant: Instant, longitude: Double): LocalDateTime {
        requireLongitude(longitude)
        val offsetMillis = (longitude * SECONDS_PER_DEGREE * 1000).roundToLong()
        return instant.plusMillis(offsetMillis).atOffset(ZoneOffset.UTC).toLocalDateTime()
    }

    /**
     * True (apparent) solar time at the given longitude:
     * local mean solar time corrected by [equationOfTime].
     *
     * @param instant Moment in time (e.g. the moment of birth).
     * @param longitude Geographic longitude in degrees, east positive, −180..180.
     */
    fun trueSolarTime(instant: Instant, longitude: Double): LocalDateTime =
        localMeanSolarTime(instant, longitude).plus(equationOfTime(instant))

    /**
     * Equation of Time (均时差): apparent solar time minus mean solar time.
     *
     * Positive in early November (sundial ahead, about +16 min),
     * negative in mid-February (about −14 min).
     *
     * Uses the NOAA approximation (Spencer series); accuracy is within
     * about 30 seconds, well below the 2-hour width of a Chinese hour.
     */
    fun equationOfTime(instant: Instant): Duration {
        val utc = instant.atOffset(ZoneOffset.UTC)
        val daysInYear = Year.of(utc.year).length()
        val fractionalHour = utc.hour + utc.minute / 60.0 + utc.second / 3600.0
        val gamma = 2 * PI / daysInYear * (utc.dayOfYear - 1 + (fractionalHour - 12) / 24)

        val minutes = 229.18 * (
            0.000075 +
                0.001868 * cos(gamma) -
                0.032077 * sin(gamma) -
                0.014615 * cos(2 * gamma) -
                0.040849 * sin(2 * gamma)
            )

        return Duration.ofMillis((minutes * 60_000).roundToLong())
    }

    private fun requireLongitude(longitude: Double) {
        require(longitude.isFinite() && longitude in -180.0..180.0) {
            "Longitude must be within -180..180 degrees, got $longitude"
        }
    }
}
