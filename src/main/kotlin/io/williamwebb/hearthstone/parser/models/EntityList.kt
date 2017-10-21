package io.williamwebb.hearthstone.parser.models

import io.reactivex.functions.Predicate
import io.williamwebb.hearthstone.parser.utils.Utils
import java.util.*

class EntityList : ArrayList<Entity>() {

    fun filter(predicate: Predicate<Entity>) = this.filter { predicate.test(it) }.toCollection(EntityList())

    fun toCardMap(): Map<String, Int> {
        val map = HashMap<String, Int>()
        for (entity in filter(HAS_CARD_ID)) {
            Utils.cardMapAdd(map, entity.CardID!!, 1)
        }
        return map
    }

    companion object {
        @JvmField val IS_IN_DECK = zone(Entity.ZONE_DECK)
        @JvmField val IS_NOT_IN_DECK = not(IS_IN_DECK)
        @JvmField val IS_IN_HAND = zone(Entity.ZONE_HAND)

        @JvmField val HAS_CARD_ID = Predicate<Entity> { entity -> entity.CardID?.isNotEmpty() ?: false && entity.card?.id != "?" }
        @JvmField val IS_FROM_ORIGINAL_DECK = Predicate<Entity> { entity -> entity.extra.originalDeck }
        @JvmField val IS_NOT_FROM_ORIGINAL_DECK = Predicate<Entity>{ entity -> !IS_FROM_ORIGINAL_DECK.test(entity) }
        private val IS_ENCHANTMENT = cardType(Entity.CARDTYPE_ENCHANTMENT)
        @JvmField val IS_NOT_ENCHANTMENT = not(IS_ENCHANTMENT)

        @JvmStatic fun <T> not(predicate: Predicate<T>) = Predicate<T> { !predicate.test(it) }
        fun cardType(cardType: String) = Predicate<Entity> { cardType == it.tags[Entity.KEY_CARDTYPE] }
        fun zone(zone: String) = Predicate<Entity> { zone == it.tags[Entity.KEY_ZONE] }
    }
}
