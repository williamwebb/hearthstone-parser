package io.williamwebb.hearthstone.parser.models

import java.lang.ref.WeakReference
import java.util.*

/**
 * Created by martin on 11/7/16.
 */
class Player(val id: String, @JvmField val entity: Entity) {

    @JvmField var battleTag: String? = null
    @JvmField var isOpponent: Boolean = false
    @JvmField var hasCoin: Boolean = false

    @JvmField var hero: Entity? = null
    @JvmField var heroPower: Entity? = null

    @JvmField var inMulligan = false

    @JvmField val listeners: MutableList<WeakReference<Listener>> = ArrayList()

    /**
     * entities controlled by this player
     */
    @JvmField val entities = EntityList()

    fun classIndex(cardDb: io.williamwebb.hearthstone.parser.CardDb): Int {
        val card = cardDb.getCard(hero!!.CardID)
        return Card.playerClassToClassIndex(card.playerClass)
    }

    fun zone(zoneId: String): EntityList {
        return entities.filter(EntityList.zone(zoneId))
    }

    fun notifyListeners() {
        val it = listeners.iterator()

        while (it.hasNext()) {
            val listener = it.next().get()
            if (listener == null) {
                it.remove()
            } else {
                listener.onPlayerStateChanged()
            }
        }
    }

    fun registerListener(listener: Listener) {
        listeners.add(WeakReference(listener))
    }

    fun unregisterListener(listener: Listener) {
        val it = listeners.iterator()
        while (it.hasNext()) {
            if (it.next() === listener) {
                it.remove()
            }
        }
    }

    interface Listener {
        fun onPlayerStateChanged()
    }

    fun reset() {
        entities.clear()
    }
}
