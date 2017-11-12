package io.williamwebb.hearthstone.parser.models

/**
 * Created by martin on 11/21/16.
 */

data class Play (
    @JvmField val cardId: String,
    @JvmField val turn: Int,
    @JvmField val isOpponent: Boolean = false
)
