package io.williamwebb.hearthstone.parser.models

import log.Logger
import java.util.*

/**
 * Created by martin on 11/8/16.
 */

class Entity(@JvmField var EntityID: String) {
    @JvmField var CardID: String? = null // might be null if the entity is not revealed yet
    @JvmField var PlayerID: String? = null // only valid for player entities

    @JvmField val tags: MutableMap<String, String> = HashMap()

    /**
     * extra information added by GameLogic
     */
    @JvmField val extra = Extra()
    @JvmField var card: Card? = null

    override fun toString(): String {
        return "Entity{" +
                "EntityID='" + EntityID + '\'' +
                ", CardID='" + CardID + '\'' +
                ", tags=" + tags +
                ", extra=" + extra +
                '}'
    }

    fun dump() {
        for (key in tags.keys) {
            Logger.d("   " + key + "=" + tags[key])
        }
    }

    companion object {
        @JvmField val KEY_ZONE = "ZONE"
        @JvmField val KEY_CONTROLLER = "CONTROLLER"
        @JvmField val KEY_CARDTYPE = "CARDTYPE"
        @JvmField val KEY_FIRST_PLAYER = "FIRST_PLAYER"
        @JvmField val KEY_PLAYSTATE = "PLAYSTATE"
        @JvmField val KEY_STEP = "STEP"
        @JvmField val KEY_TURN = "TURN"

        @JvmField val PLAYSTATE_WON = "WON"

        @JvmField val ZONE_DECK = "DECK"
        @JvmField val ZONE_HAND = "HAND"
        @JvmField val ZONE_PLAY = "PLAY"
        @JvmField val ZONE_GRAVEYARD = "GRAVEYARD"
        @JvmField val ZONE_SECRET = "SECRET"

        @JvmField val CARDTYPE_HERO = "HERO"
        @JvmField val CARDTYPE_HERO_POWER = "HERO_POWER"
        @JvmField val CARDTYPE_ENCHANTMENT = "ENCHANTMENT"
        @JvmField val CARDTYPE_GAME = "GAME"

        @JvmField val ENTITY_ID_GAME = "1"

        @JvmField val STEP_FINAL_GAMEOVER = "FINAL_GAMEOVER"
        @JvmField val STEP_BEGIN_MULLIGAN = "BEGIN_MULLIGAN"

        @JvmField val MULLIGAN_TAG_KEY = "MULLIGAN_STATE"
        @JvmField val MULLIGAN_TAG_DEALING = "DEALING"
        @JvmField val MULLIGAN_TAG_DONE = "DONE"
    }

    data class Extra(
        var originalDeck: Boolean = false,
        var drawTurn: Int = -1,
        var playTurn: Int = -1,
        var diedTurn: Int = -1,
        var mulliganed: Boolean = false,
        var createdBy: String? = null
    )
}
