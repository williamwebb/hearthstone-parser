package io.williamwebb.hearthstone.parser.models

import java.util.*

/**
 * Created by martin on 11/7/16.
 */

class Game(
        @JvmField internal val gameEntity: Entity,
        @JvmField internal val entityMap: MutableMap<String, Entity>,
        @JvmField val player: Player,
        @JvmField val opponent: Player) {

    @JvmField val playerMap = mapOf(
            player.entity.PlayerID to player,
            opponent.entity.PlayerID to opponent
    )
    @JvmField val battleTags: MutableList<String> = ArrayList()

    @JvmField val plays: MutableList<Play> = ArrayList()
    @JvmField var started: Boolean = false
    @JvmField var turn: Int = 0

    fun findController(entity: Entity) = findPlayer(entity.tags[Entity.KEY_CONTROLLER])
    fun findPlayer(playerId: String?) = playerMap[playerId]
}
