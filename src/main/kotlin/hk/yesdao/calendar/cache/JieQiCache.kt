package hk.yesdao.calendar.cache

import com.nlf.calendar.Lunar
import com.nlf.calendar.Solar
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe cache of 24 solar terms (JieQi, 节气) grouped by Gregorian year.
 *
 * The cache avoids rebuilding the JieQi table for every conversion.
 */
object JieQiCache {

    private val cache = ConcurrentHashMap<Int, Map<String, Solar>>()

    /**
     * Reference date used to build the JieQi table.
     *
     * June 1st is intentionally chosen because it always belongs to
     * the target Chinese calendar year (i.e. after Chinese New Year),
     * ensuring the correct solar-term table is produced.
     */
    private const val REFERENCE_MONTH = 6
    private const val REFERENCE_DAY = 1

    /**
     * Returns all solar terms for the specified Gregorian year.
     *
     * The data is loaded lazily on first access and then cached
     * for subsequent requests.
     */
    fun getForYear(year: Int): Map<String, Solar> =
        cache.computeIfAbsent(year) {
            val lunar = Lunar.fromYmd(year, REFERENCE_MONTH, REFERENCE_DAY)
            lunar.jieQiTable
        }

    /** Removes cached solar terms for the specified year. */
    fun clearYear(year: Int) {
        cache.remove(year)
    }

    /** Clears the entire solar-term cache. */
    fun clearAll() {
        cache.clear()
    }
}