package io.williamwebb.hearthstone.parser.parsers.power

import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import io.williamwebb.hearthstone.parser.GameLogic.entityRevealed
import io.williamwebb.hearthstone.parser.GameLogic.tagChanged
import io.williamwebb.hearthstone.parser.logreader.LogReader
import io.williamwebb.hearthstone.parser.logreader.LogReader.LineConsumer
import io.williamwebb.hearthstone.parser.logreader.LogReader.LogLine
import io.williamwebb.hearthstone.parser.models.Entity
import io.williamwebb.hearthstone.parser.models.Game
import io.williamwebb.hearthstone.parser.models.Player
import io.williamwebb.hearthstone.parser.models.Play
import io.williamwebb.hearthstone.parser.utils.PowerParserUtils.decodeEntityName
import io.williamwebb.hearthstone.parser.utils.PowerParserUtils.findEntityByName
import io.williamwebb.hearthstone.parser.utils.PowerParserUtils.getNodeTags
import io.williamwebb.hearthstone.parser.utils.PowerParserUtils.guessCardIdFromBlock
import io.williamwebb.hearthstone.parser.utils.Utils.extractMethod
import log.Logger
import java.util.*
import java.util.regex.Matcher
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.properties.Delegates

/**
 * Created by martin on 10/27/16.
 */

class PowerParser(private val logReader: LogReader, private val cardDb: io.williamwebb.hearthstone.parser.CardDb) : LineConsumer {
    private val mNodeStack = LinkedList<Node>()

    private var mCurrentNode: Node? = null
    private var mCurrentGame: Game by Delegates.notNull()
    private var initialized: Boolean = false

    private val BLOCK_START = "BLOCK_START BlockType=(.*) Entity=(.*) EffectCardId=(.*) EffectIndex=(.*) Target=(.*)".toPattern()
    private val BLOCK_END = "BLOCK_END".toPattern()

    private val GameEntityPattern = "GameEntity EntityID=(.*)".toPattern()
    private val PlayerEntityPattern = "Player EntityID=(.*) PlayerID=(.*) GameAccountId=(.*)".toPattern()

    private val FULL_ENTITY = "FULL_ENTITY - Updating (.*) CardID=(.*)".toPattern()
    private val SHOW_ENTITY = "SHOW_ENTITY - Updating Entity=(.*) CardID=(.*)".toPattern()
    private val HIDE_ENTITY = "HIDE_ENTITY - Entity=(.*) tag=(.*) value=(.*)".toPattern()
    private val TAG_CHANGE = "TAG_CHANGE Entity=(.*) tag=(.*) value=(.*)".toPattern()

    private val eventPublisher: Subject<Event> = PublishSubject.create<Event>()
    private val rawPublisher: Subject<LogLine> = PublishSubject.create<LogLine>()

    fun EVENTS() = eventPublisher
    fun RAW() = rawPublisher

    fun start(): Disposable {
        return logReader.observe().subscribe ({
            onLine(it)
        }, Logger::e)
    }

    override fun onLine(logLine: LogLine) {
        rawPublisher.onNext(logLine)

        try {
            val s = extractMethod(logLine.content)
            if (s == null) {
                Logger.e("Cannot parse line: " + logLine.content)
                return
            }

            if ("PowerTaskList.DebugPrintPower()" != s[0]) {
                return
            }

            val line = s[1]

            var spaces = 0
            while (spaces < line.length && line[spaces] == ' ') {
                spaces++
            }

            if (spaces == line.length) {
                Logger.d("empty line: " + line)
                return
            } else if (spaces % 4 != 0) {
                Logger.d("bad indentation: " + line)
                return
            }

            val node = Node(line = line.substring(spaces), depth = spaces / 4)

            var parent: Node? = null
            while (!mNodeStack.isEmpty()) {
                val node2 = mNodeStack.peekLast()
                if (node.depth == node2.depth + 1) {
                    parent = node2
                    break
                }
                mNodeStack.removeLast()
            }
            if (parent == null) {
                outputCurrentNode(mCurrentNode)
                mNodeStack.clear()
                mCurrentNode = node
            } else if (BLOCK_END.matcher(parent.line).matches()) {
                /**
                 * BLOCK_END is a special case :-/
                 */
                outputCurrentNode(mCurrentNode)
                mNodeStack.clear()
                mCurrentNode = node
            } else {
                parent.children.add(node)
            }

            mNodeStack.add(node)
        } catch (e: Exception) {
            Logger.e(e)
        }
    }

    private fun outputCurrentNode(node: Node?) {
        if (node == null) {
            return
        }

        val line = node.line
        var m: Matcher by Delegates.notNull()

        if (node.depth == 0) {
            if ((BLOCK_START.matcher(line).apply { m = this }).matches()) {
                val block = Block(
                        blockType = m.group(1),
                        entity = m.group(2),
                        effectCardId = m.group(3),
                        effectIndex = m.group(4),
                        target = m.group(5)
                )

                node.children.forEach { outputAction(it, block) }

                if (initialized && Block.TYPE_PLAY == block.blockType) {
                    val entity = findEntityByName(mCurrentGame, m.group(2))
                    io.williamwebb.hearthstone.parser.GameLogic.entityPlayed(eventPublisher, mCurrentGame, entity)
                }
            } else if ((BLOCK_END.matcher(line).apply { m = this }).matches()) {
//                printError("block end")
            } else {
                Logger.e("unknown block: " + line)
            }
        } else if (node.depth == 1) {
            outputAction(node, null)
        } else {
            Logger.e("ignore block" + line)
        }
    }

    private fun outputAction(node: Node, block: Block?) {
        val line = node.line
        var m: Matcher by Delegates.notNull()

        if (line.startsWith("CREATE_GAME")) {
            if (initialized) {
                Logger.d("CREATE_GAME during an existing one, resuming")
            } else {
                val entityMap = HashMap<String, Entity>()
                for (child in node.children) {
                    if ((GameEntityPattern.matcher(child.line).apply { m = this }).matches()) {
                        val EntityID = m.group(1)
                        val entity = Entity(EntityID)
                        entity.tags.putAll(getNodeTags(child))

                        entityMap.put(entity.EntityID, entity)
                    } else if ((PlayerEntityPattern.matcher(child.line).apply { m = this }).matches()) {
                        val EntityID = m.group(1)
                        val entity = Entity(EntityID)
                        entity.PlayerID = m.group(2)
                        entity.tags.putAll(getNodeTags(child))

                        entityMap.put(entity.EntityID, entity)
                    }
                }
                mCurrentGame = io.williamwebb.hearthstone.parser.GameLogic.gameCreated(eventPublisher, entityMap)
                initialized = true
            }
        }
        if(!initialized) {
            Logger.e("Not initialized!")
            return
        }

        val game = mCurrentGame

        if ((TAG_CHANGE.matcher(line).apply { m = this }).matches()) {
            val entityName = m.group(1).trim()
            val key = m.group(2).trim()
            val value = m.group(3).trim()

            if (key.isNotEmpty()) {
                val entity = findEntityByName(game, entityName)
                tagChange(game, entityName, entity, key, value)
            }
        } else if ((FULL_ENTITY.matcher(line).apply { m = this }).matches()) {
            val entityId = decodeEntityName(m.group(1))["id"] ?: return
            var entity: Entity? = game.entityMap[entityId]

            var isNew = false
            if (entity == null) {
                entity = Entity(entityId)
                game.entityMap.put(entityId, entity)
                isNew = true
            }
            entity.EntityID = entityId
            entity.CardID = m.group(2)
            entity.tags.putAll(getNodeTags(node))

            if (entity.CardID.isNullOrEmpty() && block != null) {
                /**
                 * this entity is created by something, try to guess
                 */
                entity.CardID = guessCardIdFromBlock(game, cardDb, entity, block)
            }

            if (!entity.CardID.isNullOrEmpty()) {
                entity.card = cardDb.getCard(entity.CardID)
            }

            if (isNew) {
                io.williamwebb.hearthstone.parser.GameLogic.entityCreated(eventPublisher, game, entity)
            }

        } else if ((SHOW_ENTITY.matcher(line).apply { m = this }).matches()) {
            val entityName = m.group(1)
            val entity = findEntityByName(game, entityName)
            val cardID = m.group(2)
            if (!entity.CardID.isNullOrEmpty() && entity.CardID != cardID) {
                Logger.e("[Inconsistent] entity " + entity + " changed cardId " + entity.CardID + " -> " + cardID)
            }
            entity.CardID = cardID
            entity.card = cardDb.getCard(cardID)

            val newTags = getNodeTags(node)
            for (key in newTags.keys) {
                tagChange(game, entityName, entity, key, newTags[key] ?: "")
            }

            entityRevealed(eventPublisher, game, entity)
        } else if ((HIDE_ENTITY.matcher(line).apply { m = this }).matches()) {
            /**
             * do nothing and rely on tag changes instead
             */
        }
    }

    private fun tagChange(game: Game, entityName: String, entity: Entity, key: String, newValue: String) {
        val oldValue = entity.tags[Entity.KEY_ZONE]
        entity.tags.put(key, newValue)

        tagChanged(eventPublisher, game, entityName, entity, key, oldValue, newValue)

        if (!game.started) {
            eventPublisher.onNext(Event.GAME_START(game))
            game.started = true
        }

        /**
         * Do not crash If we get a disconnect before the mulligan (might happen ?)
         */
        if (Entity.ENTITY_ID_GAME == entity.EntityID && game.started) {
            if (Entity.KEY_STEP == key) {
                if (Entity.STEP_FINAL_GAMEOVER == newValue) {
                    io.williamwebb.hearthstone.parser.GameLogic.gameEnd(eventPublisher, game)
                    initialized = false
                }
            }
        }
    }

    sealed class Event {
        data class GAME_CREATED(val gameEntity: Entity, val entityMap: MutableMap<String, Entity>, val player: Player, val opponent: Player) : Event()
        data class GAME_START(val game: Game) : Event()
        class MULLIGAN_START : Event()
        class MULLIGAN_END : Event()
        data class ENTITY_CREATED(val entity: Entity) : Event()
        data class ENTITY_REVEALED(val entity: Entity) : Event()
        data class ENTITY_PLAYED(val entity: Entity, val play: Play) : Event()
        data class ENTITY_ZONE_CHANGED(val entity: Entity, val oldValue: String?, val newValue: String?) : Event()
        data class GAME_END(val game: Game, val victory: Boolean) : Event()
    }

    data class Block(val blockType: String, val entity: String, val effectCardId: String, val effectIndex: String, val target: String) {
        companion object {
            internal val TYPE_PLAY = "PLAY"
            internal val TYPE_POWER = "POWER"
            internal val TYPE_TRIGGER = "TRIGGER"
        }
    }

    data class Node(val line: String, val depth: Int) {
        val children: MutableList<Node> = ArrayList()
    }
}
