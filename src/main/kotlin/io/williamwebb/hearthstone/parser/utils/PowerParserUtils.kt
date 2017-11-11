package io.williamwebb.hearthstone.parser.utils

import io.williamwebb.hearthstone.parser.models.Card
import io.williamwebb.hearthstone.parser.models.Entity
import io.williamwebb.hearthstone.parser.models.Game
import io.williamwebb.hearthstone.parser.parsers.power.PowerParser
import log.Logger
import java.util.*
import java.util.regex.Pattern

/**
 * Created by williamwebb on 6/24/17.
 */

object PowerParserUtils {
    fun decodeEntityName(name: String): Map<String, String> {
        return decodeParams(name.substring(1, name.length - 1))
    }

    private fun getEntitySafe(game: Game, entityId: String): Entity {
        return game.entityMap[entityId] ?:  return unknownEntity(entityId)
    }

    private fun unknownEntity(entityId: String): Entity {
        Logger.e("unknown entity " + entityId)
        return Entity(entityId)
    }

    private fun decodeParams(params: String): Map<String, String> {
        var end = params.length
        val map = HashMap<String, String>()

        while (true) {
            var start = end - 1

            val value: String
            while (start >= 0 && params[start] != '=') {
                start--
            }
            if (start < 0) {
                return map
            }
            value = params.substring(start + 1, end)
            end = start
            if (end < 0) {
                return map
            }
            start = end - 1
            while (start >= 0 && params[start] != ' ') {
                start--
            }
            val key: String
            if (start == 0) {
                key = params.substring(start, end)
            } else {
                key = params.substring(start + 1, end)
            }
            map.put(key.trim { it <= ' ' }, value)
            if (start == 0) {
                break
            } else {
                end = start
            }
        }

        return map
    }

    fun findEntityByName(game: Game, name: String): Entity {
        if (name.isEmpty()) {
            return unknownEntity("empty")
        } else if (name.length >= 2 && name[0] == '[' && name[name.length - 1] == ']') {
            val id = decodeEntityName(name)["id"] ?: ""
            if (id.isEmpty()) {
                return unknownEntity(name)
            } else {
                return getEntitySafe(game, id)
            }
        } else if ("GameEntity" == name) {
            return getEntitySafe(game, Entity.ENTITY_ID_GAME)
        } else {
            // this must be a battleTag
            var entity: Entity? = game.entityMap[name]
            if (entity == null) {
                Logger.d("Adding battleTag " + name)
                if (game.battleTags.size >= 2) {
                    Logger.e("[Inconsistent] too many battleTags")
                }
                game.battleTags.add(name)

                entity = Entity(name)
                game.entityMap.put(name, entity)
            }
            return entity
        }
    }

    private val TAG = Pattern.compile("tag=(.*) value=(.*)")

    fun getNodeTags(node: PowerParser.Node): Map<String, String> {
        val map = HashMap<String, String>()

        for ((line) in node.children) {
            val m = TAG.matcher(line)
            if (m.matches()) {
                val key = m.group(1)
                if (key != null) {
                    map.put(key, m.group(2))
                }
            }
        }

        return map
    }

    private fun getTargetId(game: Game, block: PowerParser.Block): String {
        return findEntityByName(game, block.target).CardID ?: ""
    }

    fun guessCardIdFromBlock(game: Game, cardDb: io.williamwebb.hearthstone.parser.CardDb, entity: Entity, block: PowerParser.Block): String {
        val e = findEntityByName(game, block.entity)
        val actionStartingCardId = e.CardID

        if (actionStartingCardId.isNullOrEmpty()) {
            return ""
        }

        var guessedId: String? = null

        if (PowerParser.Block.TYPE_POWER == block.blockType) {
            when (actionStartingCardId) {
                Card.ID_GANG_UP, Card.ID_RECYCLE, Card.SHADOWCASTER, Card.MANIC_SOULCASTER -> guessedId = getTargetId(game, block)
                Card.ID_BENEATH_THE_GROUNDS -> guessedId = Card.ID_AMBUSHTOKEN
                Card.ID_IRON_JUGGERNAUT -> guessedId = Card.ID_BURROWING_MINE_TOKEN
                Card.FORGOTTEN_TORCH -> guessedId = Card.ROARING_TORCH
                Card.CURSE_OF_RAFAAM -> guessedId = Card.CURSED
                Card.ANCIENT_SHADE -> guessedId = Card.ANCIENT_CURSE
                Card.EXCAVATED_EVIL -> guessedId = Card.EXCAVATED_EVIL
                Card.ELISE -> guessedId = Card.MAP_TO_THE_GOLDEN_MONKEY
                Card.MAP_TO_THE_GOLDEN_MONKEY -> guessedId = Card.GOLDEN_MONKEY
                Card.DOOMCALLER -> guessedId = Card.CTHUN
                Card.JADE_IDOL -> guessedId = Card.JADE_IDOL
                Card.FLAME_GEYSER, Card.FIREFLY -> guessedId = Card.FLAME_ELEMENTAL
                Card.STEAM_SURGER -> guessedId = Card.FLAME_GEYSER
                Card.RAZORPETAL_VOLLEY, Card.RAZORPETAL_LASHER -> guessedId = Card.RAZORPETAL
                Card.BURGLY_BULLY -> guessedId = Card.ID_COIN
                Card.MUKLA_TYRANT, Card.KING_MUKLA -> guessedId = Card.BANANA
                Card.JUNGLE_GIANTS -> guessedId = Card.BARNABUS
                Card.THE_MARSH_QUEEN -> guessedId = Card.QUEEN_CARNASSA
                Card.OPEN_THE_WAYGATE -> guessedId = Card.TIME_WARP
                Card.THE_LAST_KALEIDOSAUR -> guessedId = Card.GALVADON
                Card.AWAKEN_THE_MAKERS -> guessedId = Card.AMARA
                Card.CAVERNS_BELOW -> guessedId = Card.CRYSTAL_CORE
                Card.UNITE_THE_MURLOCS -> guessedId = Card.MEGAFIN
                Card.LAKKARI_SACRIFICE -> guessedId = Card.NETHER_PORTAL
                Card.FIRE_PLUME -> guessedId = Card.SULFURAS
                Card.RAPTOR_HATCHLING -> guessedId = Card.RAPTOR_PATRIARCH
                Card.DIREHORN_HATCHLING -> guessedId = Card.DIREHORN_MATRIARCH
            }
        } else if (PowerParser.Block.TYPE_TRIGGER == block.blockType) {
            when (actionStartingCardId) {
                Card.PYROS2 -> guessedId = Card.PYROS6
                Card.PYROS6 -> guessedId = Card.PYROS10
                Card.WHITE_EYES -> guessedId = Card.STORM_GUARDIAN
                Card.DEADLY_FORK -> guessedId = Card.SHARP_FORK
                Card.IGNEOUS_ELEMENTAL -> guessedId = Card.FLAME_ELEMENTAL
                Card.RHONIN -> guessedId = Card.ARCANE_MISSILE
            }
        }
        if (guessedId != null) {
            entity.CardID = guessedId
            entity.card = cardDb.getCard(guessedId)
            entity.extra.createdBy = guessedId
        }

        return guessedId ?: ""
    }
}