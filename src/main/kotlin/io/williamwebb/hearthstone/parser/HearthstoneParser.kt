package io.williamwebb.hearthstone.parser

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.williamwebb.hearthstone.parser.logreader.LogReader
import io.williamwebb.hearthstone.parser.parsers.GameState
import io.williamwebb.hearthstone.parser.parsers.GameStateHandler
import io.williamwebb.hearthstone.parser.parsers.arena.ArenaParser
import io.williamwebb.hearthstone.parser.parsers.loading.LoadingScreenParser
import io.williamwebb.hearthstone.parser.parsers.power.PowerParser

/**
 * Created by williamwebb on 8/21/17.
 */
class HearthstoneParser(clazz: Class<LogReader>, path: String, cardDb: io.williamwebb.hearthstone.parser.CardDb) {

    val power = PowerParser(LogReader.observe(clazz, path + POWER_FILE, listOf("tag=GOLD_REWARD_STATE", "End Spectator")), cardDb)
    val arena = ArenaParser(LogReader.observe(clazz, path + ARENA_FILE, emptyList()), cardDb)
    val loading = LoadingScreenParser(LogReader.observe(clazz, path + LOADING_FILE, emptyList()))

    val gameState = GameState()

    private val gameStateHandler = GameStateHandler(gameState)

    init {
        power.EVENTS().subscribe {
            gameStateHandler.handle(it) // update game state on game end
        }

        loading.EVENTS().subscribe {
            gameStateHandler.handle(it) // update game state on mode change
        }
    }

    val running: Boolean
        get() = disposable?.isDisposed ?: false

    private var disposable: Disposable? = null

    fun start() {
        disposable = CompositeDisposable(
            power.start(),
            loading.start(),
            arena.start()
        )
    }

    fun stop() {
        disposable?.dispose()
    }

    companion object {
        private val POWER_FILE = "Power.log"
        private val ARENA_FILE = "Arena.log"
        private val LOADING_FILE = "LoadingScreen.log"
    }
}