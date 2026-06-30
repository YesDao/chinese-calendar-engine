package hk.yesdao.calendar.time

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ShanghaiTimeResolverTest {

    @Test
    fun `before zi hour should keep same date`() {
        val result = ShanghaiTimeResolver.resolveZiDate(
            LocalDateTime.of(2025, 6, 1, 22, 59),
            ZoneId.of("Asia/Shanghai")
        )

        assertEquals(
            LocalDate.of(2025, 6, 1),
            result
        )
    }

    @Test
    fun `at zi hour should move to next date`() {
        val result = ShanghaiTimeResolver.resolveZiDate(
            LocalDateTime.of(2025, 6, 1, 23, 0),
            ZoneId.of("Asia/Shanghai")
        )

        assertEquals(
            LocalDate.of(2025, 6, 2),
            result
        )
    }

    @Test
    fun `during zi hour should move to next date`() {
        val result = ShanghaiTimeResolver.resolveZiDate(
            LocalDateTime.of(2025, 6, 1, 23, 59),
            ZoneId.of("Asia/Shanghai")
        )

        assertEquals(
            LocalDate.of(2025, 6, 2),
            result
        )
    }

    @Test
    fun `midnight should stay on same date`() {
        val result = ShanghaiTimeResolver.resolveZiDate(
            LocalDateTime.of(2025, 6, 2, 0, 0),
            ZoneId.of("Asia/Shanghai")
        )

        assertEquals(
            LocalDate.of(2025, 6, 2),
            result
        )
    }

    @Test
    fun `null zone should use shanghai`() {
        val result = ShanghaiTimeResolver.resolveZiDate(
            LocalDateTime.of(2025, 6, 1, 23, 0),
            null
        )

        assertEquals(
            LocalDate.of(2025, 6, 2),
            result
        )
    }

    @Test
    fun `should convert source timezone before applying zi hour rule`() {
        val result = ShanghaiTimeResolver.resolveZiDate(
            LocalDateTime.of(2025, 6, 1, 16, 0),
            ZoneId.of("UTC")
        )

        // 16:00 UTC = 00:00 Asia/Shanghai следующего дня
        assertEquals(
            LocalDate.of(2025, 6, 2),
            result
        )
    }

    @Test
    fun `before zi hour should keep same date (zdt)`() {
        val zdt = ZonedDateTime.of(
            2025, 6, 1,
            22, 59, 0, 0,
            ZoneId.of("Asia/Shanghai")
        )

        assertEquals(
            LocalDate.of(2025, 6, 1),
            ShanghaiTimeResolver.resolveZiDate(zdt)
        )
    }

    @Test
    fun `resolveZiDate zdt at zi hour should move to next date`() {
        val zdt = ZonedDateTime.of(
            2025, 6, 1,
            23, 0, 0, 0,
            ZoneId.of("Asia/Shanghai")
        )

        assertEquals(
            LocalDate.of(2025, 6, 2),
            ShanghaiTimeResolver.resolveZiDate(zdt)
        )
    }

    @Test
    fun `resolveZiDate zdt during zi hour should move to next date`() {
        val zdt = ZonedDateTime.of(
            2025, 6, 1,
            23, 59, 59, 0,
            ZoneId.of("Asia/Shanghai")
        )

        assertEquals(
            LocalDate.of(2025, 6, 2),
            ShanghaiTimeResolver.resolveZiDate(zdt)
        )
    }

    @Test
    fun `midnight should stay on same date (zdt)`() {
        val zdt = ZonedDateTime.of(
            2025, 6, 2,
            0, 0, 0, 0,
            ZoneId.of("Asia/Shanghai")
        )

        assertEquals(
            LocalDate.of(2025, 6, 2),
            ShanghaiTimeResolver.resolveZiDate(zdt)
        )
    }

    @Test
    fun `last minute before zi hour should keep same date`() {
        val zdt = ZonedDateTime.of(
            2025, 6, 1,
            22, 59, 59, 0,
            ZoneId.of("Asia/Shanghai")
        )

        assertEquals(
            LocalDate.of(2025, 6, 1),
            ShanghaiTimeResolver.resolveZiDate(zdt)
        )
    }

    @ParameterizedTest
    @CsvSource(
        "23, 0",
        "0, 0",
        "1, 1",
        "2, 1",
        "3, 2",
        "4, 2",
        "5, 3",
        "6, 3",
        "7, 4",
        "8, 4",
        "9, 5",
        "10, 5",
        "11, 6",
        "12, 6",
        "13, 7",
        "14, 7",
        "15, 8",
        "16, 8",
        "17, 9",
        "18, 9",
        "19, 10",
        "20, 10",
        "21, 11",
        "22, 11"
    )
    fun `should calculate zi hour index`(
        hour: Int,
        expectedIndex: Int
    ) {
        val dateTime = LocalDateTime.of(
            2025, 6, 1,
            hour, 0
        )

        assertEquals(
            expectedIndex,
            ShanghaiTimeResolver.calculateZiHourIndex(dateTime)
        )
    }

    @Test
    fun `should build key before zi hour`() {
        val dt = LocalDateTime.of(
            2025, 6, 1,
            22, 0
        )

        assertEquals(
            "20250601Z11",
            ShanghaiTimeResolver.fastZiHourKey(dt)
        )
    }

    @Test
    fun `should build key at zi hour`() {
        val dt = LocalDateTime.of(
            2025, 6, 1,
            23, 0
        )

        assertEquals(
            "20250602Z0",
            ShanghaiTimeResolver.fastZiHourKey(dt)
        )
    }

    @Test
    fun `should build key after midnight`() {
        val dt = LocalDateTime.of(
            2025, 6, 2,
            0, 0
        )

        assertEquals(
            "20250602Z0",
            ShanghaiTimeResolver.fastZiHourKey(dt)
        )
    }

    @Test
    fun `should preserve leading zeros in month and day`() {
        val dt = LocalDateTime.of(
            2025, 1, 5,
            23, 0
        )

        assertEquals(
            "20250106Z0",
            ShanghaiTimeResolver.fastZiHourKey(dt)
        )
    }

    @Test
    fun `should build key before zi hour (zdt)`() {
        val zdt = ZonedDateTime.of(
            2025, 6, 1,
            22, 0, 0, 0,
            ZoneId.of("Asia/Shanghai")
        )

        assertEquals(
            "20250601Z11",
            ShanghaiTimeResolver.ziHourKey(zdt)
        )
    }
    @Test
    fun `should build key at zi hour (zdt)`() {
        val zdt = ZonedDateTime.of(
            2025, 6, 1,
            23, 0, 0, 0,
            ZoneId.of("Asia/Shanghai")
        )

        assertEquals(
            "20250602Z0",
            ShanghaiTimeResolver.ziHourKey(zdt)
        )
    }
    @Test
    fun `should preserve leading zeros`() {
        val zdt = ZonedDateTime.of(
            2025, 1, 5,
            23, 0, 0, 0,
            ZoneId.of("Asia/Shanghai")
        )

        assertEquals(
            "20250106Z0",
            ShanghaiTimeResolver.ziHourKey(zdt)
        )
    }

}