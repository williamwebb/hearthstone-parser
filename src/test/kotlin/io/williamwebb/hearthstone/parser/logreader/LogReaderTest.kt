package io.williamwebb.hearthstone.parser.logreader

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Created by williamwebb on 6/21/17.
 */
class LogReaderTest {

    private val time_line = "19:24:38.3618870"
    private val raw_time_line = "D " + time_line
    private val content_line = "PowerTaskList.DebugPrintPower() -     TAG_CHANGE Entity=jug6ernaut tag=MULLIGAN_STATE value=DONE"
    private val raw_line = raw_time_line + " " + content_line

    @Test
    fun testFindEntryPoint() {
        val powerLog = this.javaClass.getResource("../power.log").readText()

        val logline = findEntryPoint(listOf("tag=GOLD_REWARD_STATE", "End Spectator"), powerLog.lines())

        System.out.println("${logline.time} " + logline.line)

        assertEquals(71404455000, logline.time)
    }

    @Test
    fun testDecodeTime() {
        val decoded = toEpoch(time_line)
        assertEquals(69881618870L, decoded)
    }

    @Test
    fun testParseStringForTime() {
        var parsed = parseTime(raw_time_line)
        assertEquals("19:24:38.3618870", parsed)

        parsed = parseTime(raw_time_line.substring(0, raw_time_line.length - 1))
        assertEquals("", parsed)
    }

    @Test
    fun testParseStringForContent() {
        var parsed = parseContent(raw_line)
        assertEquals (content_line, parsed)

        parsed = parseContent("_".repeat(19))
        assertEquals ("", parsed)
    }
}