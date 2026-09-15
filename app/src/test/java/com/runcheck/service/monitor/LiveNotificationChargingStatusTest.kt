package com.runcheck.service.monitor

import com.runcheck.R
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.testutil.findAppDir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class LiveNotificationChargingStatusTest {
    @Test
    fun `enabled charging status row labels charging`() {
        assertStatusLabel(
            status = ChargingStatus.CHARGING,
            expectedText = "Charging",
            expectedResource = R.string.charging_status_charging,
        )
    }

    @Test
    fun `enabled charging status row labels discharging`() {
        assertStatusLabel(
            status = ChargingStatus.DISCHARGING,
            expectedText = "Discharging",
            expectedResource = R.string.charging_status_discharging,
        )
    }

    @Test
    fun `enabled charging status row labels full`() {
        assertStatusLabel(
            status = ChargingStatus.FULL,
            expectedText = "Full",
            expectedResource = R.string.charging_status_full,
        )
    }

    @Test
    fun `enabled charging status row labels not charging`() {
        assertStatusLabel(
            status = ChargingStatus.NOT_CHARGING,
            expectedText = "Not Charging",
            expectedResource = R.string.charging_status_not_charging,
        )
    }

    @Test
    fun `disabled charging status row is omitted`() {
        assertNull(
            liveNotificationChargingStatusLabelRes(ChargingStatus.FULL, enabled = false),
        )
    }

    private fun assertStatusLabel(
        status: ChargingStatus,
        expectedText: String,
        expectedResource: Int,
    ) {
        val actualResource = requireNotNull(liveNotificationChargingStatusLabelRes(status, enabled = true))
        assertEquals(expectedResource, actualResource)
        assertEquals(expectedText, chargingStatusStrings().getValue(actualResource))
    }

    private fun chargingStatusStrings(): Map<Int, String> {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(findAppDir().resolve("src/main/res/values/strings.xml").toFile())
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length)
            .map { nodes.item(it) as Element }
            .filter { it.getAttribute("name").startsWith("charging_status_") }
            .associate { element ->
                R.string::class.java.getField(element.getAttribute("name")).getInt(null) to element.textContent
            }
    }
}
