package io.williamwebb.hearthstone.parser.parsers

import io.williamwebb.hearthstone.parser.models.Game
import io.williamwebb.hearthstone.parser.parsers.loading.LoadingScreenParser

/**
 * Created by williamwebb on 8/18/17.
 */
class GameState {
    var mode = LoadingScreenParser.MODE_OTHER
    var lastGame: Game? = null
}