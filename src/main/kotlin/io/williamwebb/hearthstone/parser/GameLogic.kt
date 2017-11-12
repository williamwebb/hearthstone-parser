package io.williamwebb.hearthstone.parser

import io.reactivex.subjects.Subject
import io.williamwebb.hearthstone.parser.models.Entity
import io.williamwebb.hearthstone.parser.models.Game
import io.williamwebb.hearthstone.parser.models.Play
import io.williamwebb.hearthstone.parser.models.Player
import io.williamwebb.hearthstone.parser.parsers.power.PowerParser
import log.Logger

/**
 * Created by williamwebb on 6/24/17.
 */

object GameLogic {

    fun entityPlayed(publisher: Subject<PowerParser.Event>, game: Game, entity: Entity) {
        val turn = game.gameEntity.tags[Entity.KEY_TURN]
        if (turn == null) {
            Logger.e("cannot get turn")
            return
        }
        if (entity.CardID == null) {
            Logger.e("no CardID for play")
            return
        }

        val play = Play(entity.CardID!!, Integer.parseInt(turn.trim()), game.findController(entity)!!.isOpponent)

        Logger.d("%s played %s", if (play.isOpponent) "opponent" else "I", play.cardId)

        game.plays.add(play)
        publisher.onNext(PowerParser.Event.ENTITY_PLAYED(entity, play))
    }

    fun gameCreated(publisher: Subject<PowerParser.Event>, entityMap: MutableMap<String, Entity>): Game {
        var gameEntity: Entity? = null

        var player: Player? = null
        var opponent: Player? = null

        for (entity in entityMap.values) {
            if (entity.PlayerID != null) {
                Logger.d("adding player " + entity.PlayerID!!)
                if (entity.PlayerID == "1") {
                    player = Player("1", entity)
                } else {
                    opponent = Player("2", entity)
                }
            } else if (Entity.ENTITY_ID_GAME == entity.EntityID) {
                gameEntity = entity
            }
        }

        publisher.onNext(PowerParser.Event.GAME_CREATED(gameEntity!!, entityMap, player!!, opponent!!))
        return Game(gameEntity, entityMap, player, opponent)
    }

    fun gameEnd(publisher: Subject<PowerParser.Event>, game: Game) {
        val victory = Entity.PLAYSTATE_WON == game.player.entity.tags[Entity.KEY_PLAYSTATE]
        publisher.onNext(PowerParser.Event.GAME_END(game, victory))

        val player1 = game.playerMap["1"]
        val player2 = game.playerMap["2"]

        if(player1 != null) {
            player1.entities.clear()
            player1.notifyListeners()
        }

        if(player2 != null) {
            player2.entities.clear()
            player2.notifyListeners()
        }
    }

    fun entityRevealed(publisher: Subject<PowerParser.Event>, game: Game, entity: Entity) {
        publisher.onNext(PowerParser.Event.ENTITY_REVEALED(entity))
        game.findController(entity)?.notifyListeners()
    }

    fun entityCreated(publisher: Subject<PowerParser.Event>, game: Game, entity: Entity) {
        publisher.onNext(PowerParser.Event.ENTITY_CREATED(entity))

        val playerId = entity.tags[Entity.KEY_CONTROLLER]
        val cardType = entity.tags[Entity.KEY_CARDTYPE]
        val player = game.findController(entity)

        if(player == null) {
            Logger.e("unable to get player for entity: " + entity)
            return
        }

        Logger.d("entity created %s controller=%s zone=%s ", entity.EntityID, playerId, entity.tags[Entity.KEY_ZONE])

        if (Entity.CARDTYPE_HERO == cardType) {
            player.hero = entity
        } else if (Entity.CARDTYPE_HERO_POWER == cardType) {
            player.heroPower = entity
        } else {
            player.entities.add(entity)

            if (game.gameEntity.tags[Entity.KEY_STEP] == null && Entity.ZONE_DECK == entity.tags[Entity.KEY_ZONE]) {
                entity.extra.originalDeck = true
            }
        }

        player.notifyListeners()
    }

    private fun gameStepBeginMulligan(publisher: Subject<PowerParser.Event>, game: Game) {
        var knownCardsInHand = 0
        var totalCardsInHand = 0

        val player1 = game.playerMap["1"]
        val player2 = game.playerMap["2"]

        if (player1 == null || player2 == null) {
            Logger.e("cannot find players")
            return
        }

        player1.inMulligan = true
        player2.inMulligan = true

        for (entity in player1.zone(Entity.ZONE_HAND)) {
            if (!entity.CardID.isNullOrEmpty()) {
                knownCardsInHand++
            }
            totalCardsInHand++
        }

        player1.isOpponent = knownCardsInHand < 3
        player1.hasCoin = totalCardsInHand > 3

        player2.isOpponent = !player1.isOpponent
        player2.hasCoin = !player1.hasCoin

        /**
         * now try to match a battle tag with a player
         */
        for (battleTag in game.battleTags) {
            val battleTagEntity = game.entityMap[battleTag] ?: continue
            val playsFirst = battleTagEntity.tags[Entity.KEY_FIRST_PLAYER]
            val player: Player

            player = if ("1" == playsFirst) {
                if (player1.hasCoin) player2 else player1
            } else {
                if (player1.hasCoin) player1 else player2
            }

            player.entity.tags.putAll(battleTagEntity.tags)
            player.battleTag = battleTag

            /**
             * make the battleTag point to the same entity..
             */
            Logger.d(battleTag + " now points to entity " + player.entity.EntityID)
            game.entityMap.put(battleTag, player.entity)
        }
        publisher.onNext(PowerParser.Event.MULLIGAN_START())
    }

    private fun gameStepEndMulligan(publisher: Subject<PowerParser.Event>, game: Game, entity: String) {
        val playerId = game.entityMap[entity]?.PlayerID ?: return
        val player = game.findPlayer(playerId) ?: return

        player.inMulligan = false
        player.notifyListeners()

        publisher.onNext(PowerParser.Event.MULLIGAN_END())
    }

    fun tagChanged(publisher: Subject<PowerParser.Event>, game: Game, entityName: String, entity: Entity, key: String, oldValue: String?, newValue: String?) {
        if (Entity.ENTITY_ID_GAME == entity.EntityID) {
            if (Entity.KEY_TURN == key) {
                try {
                    game.turn = (Integer.parseInt(newValue) + 1) / 2
                    Logger.d("turn: " + game.turn)
                } catch (e: Exception) {
                    Logger.e(e)
                }
            }

            if (Entity.KEY_STEP == key) {
                if (Entity.STEP_BEGIN_MULLIGAN == newValue) {
                    io.williamwebb.hearthstone.parser.GameLogic.gameStepBeginMulligan(publisher, game)
                }
            }
        } else if (key == Entity.MULLIGAN_TAG_KEY && newValue == Entity.MULLIGAN_TAG_DONE) {
            io.williamwebb.hearthstone.parser.GameLogic.gameStepEndMulligan(publisher, game, entityName)
        }

        if (Entity.KEY_ZONE == key) {
            if (Entity.ZONE_DECK == oldValue && Entity.ZONE_HAND == newValue) {
                val step = game.gameEntity.tags[Entity.KEY_STEP]
                when (step) {
                    null -> entity.extra.drawTurn = 0 // this is the original mulligan
                    Entity.STEP_BEGIN_MULLIGAN -> {
                        entity.extra.drawTurn = 0
                        entity.extra.mulliganed = true
                    }
                    else -> entity.extra.drawTurn = game.turn
                }
            } else if (Entity.ZONE_HAND == oldValue && Entity.ZONE_PLAY == newValue) {
                entity.extra.playTurn = game.turn
            } else if (Entity.ZONE_HAND == oldValue && Entity.ZONE_SECRET == newValue) {
                entity.extra.playTurn = game.turn
            } else if (Entity.ZONE_PLAY == oldValue && Entity.ZONE_GRAVEYARD == newValue) {
                entity.extra.diedTurn = game.turn
            } else if (Entity.ZONE_HAND == oldValue && Entity.ZONE_DECK == newValue) {
                /**
                 * card was put back in the deck (most likely from mulligan)
                 */
                entity.extra.drawTurn = -1
            }
            publisher.onNext(PowerParser.Event.ENTITY_ZONE_CHANGED(entity, oldValue, newValue))
        }
        game.playerMap.forEach { (_, player) -> player.notifyListeners() }
    }
}