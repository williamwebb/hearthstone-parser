package io.williamwebb.hearthstone.parser.parsers

import io.williamwebb.hearthstone.parser.models.Game
import io.williamwebb.hearthstone.parser.parsers.loading.LoadingScreenParser
import io.williamwebb.hearthstone.parser.parsers.power.PowerParser

/**
 * Created by williamwebb on 8/18/17.
 */
class GameStateHandler(private val gameState: GameState) {
    var mode: Int = 0
        private set

    fun handle(event: PowerParser.Event) {
        when(event) {
            is PowerParser.Event.GAME_END -> onGameEnded(event.game, event.victory)
        }
    }

    fun handle(event: LoadingScreenParser.Event) {
        when(event) {
            is LoadingScreenParser.Event.MODE_CHANGED -> modeChanged(event.mode)
        }
    }

    private fun modeChanged(newMode: Int) {
        gameState.mode = newMode
    }

    private fun onGameEnded(game: Game, victory: Boolean) {
        gameState.lastGame = game
    }
}
