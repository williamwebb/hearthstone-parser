package io.williamwebb.hearthstone.parser

import io.reactivex.observers.TestObserver
import io.williamwebb.hearthstone.parser.logreader.LogReader
import io.williamwebb.hearthstone.parser.parsers.power.PowerParser
import org.junit.Test
import java.io.PrintWriter

/**
 * Created by williamwebb on 6/25/17.
 */
class PowerParserTest {

    init {
//        Logger.plant(Logger.DebugTree())
    }

    val power_log = this.javaClass.getResource("power.log").readText()
    private val cardDB = io.williamwebb.hearthstone.parser.CardDb(object : io.williamwebb.hearthstone.parser.CardDb.Cache {
        override fun save(key: String, json: String?) {
            val writer = PrintWriter("card_db.txt", "UTF-8")
            writer.print(json)
            writer.close()
        }

        override fun load(key: String): String? {
            return try {
                this.javaClass.getResource("card_db.txt").readText()
            } catch (e: Exception) {
                null
            }
        }
    })

    private val fileReader = object : LogReader("powerlog", emptyList()) {
        override fun readEntireFile() = emptyList<String>()
        override fun onStart() = power_log.lines().forEach { post(it) }
        override fun onStop() { }
    }

    private val power = PowerParser(fileReader, cardDB)

    @Test
    fun testRawPublisher() {
        val rawObserver = TestObserver<LogReader.LogLine>()

        power.RAW().subscribe(rawObserver)

        power.start()
        fileReader.observe().subscribe()

        rawObserver.assertNoErrors()
        rawObserver.assertValueCount(10841)
    }

    @Test
    fun testEventPublisher() {
        val eventObserver = TestObserver<PowerParser.Event>()

        power.EVENTS().subscribe(eventObserver)

        power.start()
        fileReader.observe().subscribe()

        eventObserver.assertValueCount(281)

        eventObserver.assertValueAt(0, { it is PowerParser.Event.GAME_CREATED })
        assert (eventObserver.values().filterIndexed { index, event -> index in 1..64 && event is PowerParser.Event.ENTITY_CREATED }.size == 64)
        eventObserver.assertValueAt(65, { it is PowerParser.Event.GAME_START })

        eventObserver.assertValueAt(77, { it is PowerParser.Event.MULLIGAN_START })
        eventObserver.assertValueAt(82, { it is PowerParser.Event.MULLIGAN_END })

        eventObserver.assertValueAt(280, { it is PowerParser.Event.GAME_END })

//        eventObserver.values().forEachIndexed { i, t -> System.out.println("$i " + t) }
    }

    @Test
    fun testPlayerTurnDrawn() {
        val eventObserver = TestObserver<Pair<String?, Int>>()

        power.EVENTS()
                .filter { it is PowerParser.Event.ENTITY_ZONE_CHANGED && it.newValue == "HAND" }
                .map { it.cast(PowerParser.Event.ENTITY_ZONE_CHANGED::class.java) }
                .filter { it.entity.tags["CONTROLLER"] == "1" }
                .filter { it.entity.extra.drawTurn != -1 }
                .map { it.entity.CardID to it.entity.extra.drawTurn }
                .subscribe(eventObserver)

        power.start()
        fileReader.observe().subscribe()

        eventObserver.assertValueCount(16)
        eventObserver.assertValueSequence(listOf(
                "UNG_011" to 0,
                "NEW1_020" to 0,
                "UNG_015" to 0,
                "CS2_097" to 0,
                "NEW1_020" to 1,
                "CS2_097" to 2,
                "EX1_383" to 3,
                "EX1_619" to 4,
                "KAR_061" to 5,
                "UNG_952" to 6,
                "OG_198" to 7,
                "NEW1_041" to 7,
                "UNG_848" to 7,
                "UNG_011" to 7,
                "UNG_015" to 8,
                "UNG_072" to 9
        ))
    }

    @Test
    fun testOpponentTurnDrawn() {
        val eventObserver = TestObserver<Pair<Int, Boolean>>()

        power.EVENTS()
                .filter { it is PowerParser.Event.ENTITY_ZONE_CHANGED && it.newValue == "HAND" }
                .map { it.cast(PowerParser.Event.ENTITY_ZONE_CHANGED::class.java) }
                .filter { it.entity.tags["CONTROLLER"] == "2" }
                .filter { it.entity.extra.drawTurn != -1 }
                .map { it.entity.extra.drawTurn to it.entity.extra.mulliganed}
                .subscribe(eventObserver)

        power.start()
        fileReader.observe().subscribe()

        eventObserver.assertValueCount(17)
        eventObserver.assertValueSequence(listOf(
                0 to false,
                0 to false,
                0 to false,
                0 to false,
                0 to true,
                0 to true,
                1 to false,
                2 to false,
                3 to false,
                4 to false,
                5 to false,
                6 to false,
                7 to false,
                8 to false,
                9 to false,
                9 to false,
                9 to false
        ))
    }

    @Test
    fun testPlayerPlays() {
        val eventObserver = TestObserver<Pair<String, Int>>()

        power.EVENTS()
                .filter { it is PowerParser.Event.ENTITY_PLAYED }
                .map { it.cast(PowerParser.Event.ENTITY_PLAYED::class.java) }
                .filter{ !it.play.isOpponent }
                .map { it.play.cardId to it.play.turn }
                .subscribe(eventObserver)

        power.start()
        fileReader.observe().subscribe()

        eventObserver.assertValueCount(10)
        eventObserver.assertValueSequence(listOf(
                "UNG_011" to 3,
                "NEW1_020" to 5,
                "EX1_130" to 5,
                "CS2_097" to 7,
                "CS2_097" to 9,
                "CS2_101" to 11,
                "KAR_061" to 13,
                "UNG_848" to 15,
                "UNG_072" to 17,
                "UNG_015" to 17
        ))
    }

    @Test
    fun testOpponentPlays() {
        val eventObserver = TestObserver<Pair<String, Int>>()

        power.EVENTS()
                .filter { it is PowerParser.Event.ENTITY_PLAYED }
                .map { it.cast(PowerParser.Event.ENTITY_PLAYED::class.java) }
                .filter { it.play.isOpponent }
                .map { it.play.cardId to it.play.turn }
                .subscribe(eventObserver)

        power.start()
        fileReader.observe().subscribe()

        eventObserver.assertValueCount(20)
        eventObserver.assertValueSequence(listOf(
                "KAR_069" to 2,
                "GAME_005" to 4,
                "EX1_134" to 4,
                "UNG_058" to 6,
                "UNG_057t1" to 6,
                "CFM_637" to 8,
                "CS2_083b_H1" to 8,
                "CS2_073" to 8,
                "CS2_083b_H1" to 10,
                "UNG_065" to 12,
                "CS2_083b_H1" to 12,
                "CFM_630" to 14,
                "UNG_064" to 14,
                "EX1_145" to 14,
                "EX1_124" to 14,
                "CS2_083b_H1" to 14,
                "UNG_004" to 16,
                "EX1_095" to 18,
                "UNG_856" to 18,
                "OG_223" to 18
        ))
    }

    private inline fun <reified T> Any.cast(t: Class<T>): T {
        return this as T
    }
}