package neth.iecal.curbox.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for "every active day": in everyday mode the shared schedule applies only to the days in
 * [AppTimeConfig.activeDays] (null = every day, the legacy default).
 */
class AppTimeConfigTest {

    private val nineToFive = mutableListOf(TimeInterval(9, 0, 17, 0))

    @Test
    fun `everyday with null activeDays applies to every day`() {
        val config = AppTimeConfig(isEveryday = true, everydayIntervals = nineToFive, activeDays = null)
        (0..6).forEach { day ->
            assertEquals("day $day", nineToFive, config.intervalsForDay(day))
        }
    }

    @Test
    fun `everyday applies only to active days`() {
        // Weekdays only (Sunday=0 .. Saturday=6): Mon..Fri active, weekend off.
        val config = AppTimeConfig(
            isEveryday = true,
            everydayIntervals = nineToFive,
            activeDays = listOf(1, 2, 3, 4, 5)
        )
        assertTrue(config.intervalsForDay(0).isEmpty())   // Sunday off
        assertEquals(nineToFive, config.intervalsForDay(1)) // Monday on
        assertEquals(nineToFive, config.intervalsForDay(5)) // Friday on
        assertTrue(config.intervalsForDay(6).isEmpty())   // Saturday off
    }

    @Test
    fun `everyday with empty active days blocks nothing`() {
        val config = AppTimeConfig(isEveryday = true, everydayIntervals = nineToFive, activeDays = emptyList())
        (0..6).forEach { day -> assertTrue(config.intervalsForDay(day).isEmpty()) }
    }

    @Test
    fun `non-everyday uses per-day intervals and ignores active days`() {
        val config = AppTimeConfig(
            isEveryday = false,
            dailyIntervals = mutableMapOf(2 to nineToFive),
            activeDays = emptyList()
        )
        assertEquals(nineToFive, config.intervalsForDay(2))
        assertTrue(config.intervalsForDay(3).isEmpty())
    }
}
