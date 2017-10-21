package io.williamwebb.hearthstone.parser.parsers.arena

import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject.create
import io.williamwebb.hearthstone.parser.logreader.LogReader
import io.williamwebb.hearthstone.parser.models.Card
import io.williamwebb.hearthstone.parser.models.Deck
import log.Logger

/**
 * Created by martin on 11/7/16.
 */

class ArenaParser(private val logReader: LogReader, private val cardDb: io.williamwebb.hearthstone.parser.CardDb) {
//    private val DraftManager_OnBegin = "DraftManager.OnBegin - Got new draft deck with ID: (.*)".toPattern()
    private val DraftManager_OnChose = "DraftManager.OnChosen\\(\\): hero=(.*) premium=NORMAL".toPattern()
    private val Client_chooses = "Client chooses: .* \\((.*)\\)".toPattern()
    private val DraftManager_OnChoicesAndContents = "DraftManager.OnChoicesAndContents - Draft deck contains card (.*)".toPattern()

    private val eventPublisher = create<Event>()
    private val rawPublisher = create<LogReader.LogLine>()

    private var deck = Deck()

    fun EVENTS() = eventPublisher
    fun RAW() = rawPublisher

    fun start(): Disposable {
        return logReader.observe().subscribe ({
            onLine(it)
        }, Logger::e)
    }

    sealed class Event {
        data class DRAFT_STARTED(val classIndex: Int) : Event()
        data class ADD_CARD(val cardId: String) : Event()
        data class DRAFT_ENDED(val deck: Deck) : Event()
    }

    private fun onLine(logLine: LogReader.LogLine) {
        rawPublisher.onNext(logLine)

        val line = logLine.content
        Logger.d(line)

// We don't need this event, we can handle this with the hero select
//        var matcher = DraftManager_OnBegin.matcher(line)
//        if (matcher.matches()) {
//            eventPublisher.onNext(Event.CLEAR())
//            return
//        }

        var matcher = DraftManager_OnChose.matcher(line)
        if (matcher.matches()) {
            draftStarted(Card.heroIdToClassIndex(matcher.group(1)))
            return
        }

        matcher = Client_chooses.matcher(line)
        if (matcher.matches()) {
            val cardId = matcher.group(1)
            if (cardId.toLowerCase().startsWith("hero_")) {
                // This must be a hero ("Client chooses: Tyrande Whisperwind (HERO_09a)")
                Logger.e("skip hero " + cardId)
            } else {
                addCard(cardId)
            }
        }

        matcher = DraftManager_OnChoicesAndContents.matcher(line)
        if (matcher.matches()) {
            val cardId = matcher.group(1)
            addCardIfNotPresent(cardId)
        }
    }

    private fun draftStarted(classIndex: Int) {
        deck.clear()
        deck.classIndex = classIndex

        eventPublisher.onNext(Event.DRAFT_STARTED(classIndex))
    }

    private fun addCard(cardId: String) {
        deck.addCard(cardId, 1)
        eventPublisher.onNext(Event.ADD_CARD(cardId))

        if(deck.cardCount == 30) {
            eventPublisher.onNext(Event.DRAFT_ENDED(deck.copy()))
        }
    }

    private fun addCardIfNotPresent(cardId: String) {
        val card = cardDb.getCard(cardId)
        if (!deck.cards.containsKey(cardId)) {
            if (card.collectible) {
                addCard(cardId)
            } else {
                Logger.e("not collectible2 " + cardId)
            }
        }
    }
}
