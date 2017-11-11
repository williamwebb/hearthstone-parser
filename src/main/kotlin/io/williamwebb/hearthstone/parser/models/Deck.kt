package io.williamwebb.hearthstone.parser.models

import java.lang.ref.WeakReference
import java.util.*

/**
 * Created by martin on 10/17/16.
 */

data class Deck (
    @JvmField var classIndex: Int = -1,
    @JvmField var name: String? = null,
    @JvmField var id: String? = null,
    @JvmField var cards: MutableMap<String, Int> = HashMap(),
    @JvmField var wins: Int = 0,
    @JvmField var losses: Int = 0){

    @Transient private var mListenerRef: WeakReference<Listener>? = null

    interface Listener {
        fun onDeckChanged()
    }

    fun addCard(cardId: String, add: Int) {
        if (add > 0 && cardCount >= 30) {
            return
        } else if (add < 0 && cardCount <= 0) {
            return
        }

        val a: Int = (cards[cardId] ?: 0) + add

        when {
            a < 0 -> return
            a == 0 -> cards.remove(cardId)
            else -> cards.put(cardId, a)
        }

        mListenerRef?.get()?.onDeckChanged()
    }

    val cardCount: Int
        get() = cards.values.sum()

    fun setListener(listener: Listener) {
        mListenerRef = WeakReference(listener)
    }

    fun clear() {
        wins = 0
        losses = 0
        cards.clear()
        mListenerRef?.get()?.onDeckChanged()
    }

    companion object {
        @JvmField val MAX_CARDS = 30
    }
}
