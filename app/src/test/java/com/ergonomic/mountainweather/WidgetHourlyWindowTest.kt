package com.ergonomic.mountainweather

import com.ergonomic.mountainweather.widget.HourlyHourSnapshot
import com.ergonomic.mountainweather.widget.WidgetHourlyWindow
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class WidgetHourlyWindowTest {

    private fun makeHours(date: LocalDate, hours: IntRange): List<HourlyHourSnapshot> =
        hours.map { hour ->
            val time = "%sT%02d:00".format(date.toString(), hour)
            HourlyHourSnapshot(time = time, temperature = 10.0, weatherCode = 0, precipitation = 0.0)
        }

    @Test
    fun `morning shows current plus next future hours`() {
        val today = LocalDate.of(2026, 6, 17)
        val cached = makeHours(today, 0..23)
        val now = LocalDateTime.of(today, java.time.LocalTime.of(8, 30))

        val result = WidgetHourlyWindow.filterHoursForWidget(cached, now)
        val labels = result.map { it.time.takeLast(5) }

        assertEquals(WidgetHourlyWindow.MAX_HOURS, result.size)
        assertEquals("08:00", labels.first())
        assertEquals("15:00", labels.last())
    }

    @Test
    fun `late evening pads with earlier hours so current is on the right`() {
        val today = LocalDate.of(2026, 6, 17)
        val cached = makeHours(today, 0..23)
        val now = LocalDateTime.of(today, java.time.LocalTime.of(23, 40))

        val result = WidgetHourlyWindow.filterHoursForWidget(cached, now)
        val labels = result.map { it.time.takeLast(5) }

        assertEquals(WidgetHourlyWindow.MAX_HOURS, result.size)
        assertEquals("23:00", labels.last())
        assertEquals("16:00", labels.first())
    }

    @Test
    fun `cache spilling into tomorrow keeps full window after current hour`() {
        val today = LocalDate.of(2026, 6, 17)
        val tomorrow = today.plusDays(1)
        val cached = makeHours(today, 22..23) + makeHours(tomorrow, 0..6)
        val now = LocalDateTime.of(today, java.time.LocalTime.of(22, 10))

        val result = WidgetHourlyWindow.filterHoursForWidget(cached, now)
        val labels = result.map { it.time.takeLast(5) }

        assertEquals(WidgetHourlyWindow.MAX_HOURS, result.size)
        assertEquals("22:00", labels.first())
        assertEquals("05:00", labels.last())
    }
}
