package io.williamwebb.hearthstone.parser

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.williamwebb.hearthstone.parser.models.Card
import io.williamwebb.hearthstone.parser.models.Deck
import log.Logger
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Created by martin on 11/10/16.
 */

class CardDb @JvmOverloads constructor(private val cache: io.williamwebb.hearthstone.parser.CardDb.Cache, locale: Locale = Locale.getDefault()) {
    private var sCardList: List<Card>? = null
    private var cardMap: MutableMap<Int, Card> = HashMap()
    private var isReady: Boolean = false
        private set

    interface Cache {
        fun save(key: String, json: String?)
        fun load(key: String): String?
    }

    init {
        val language = locale.language.toLowerCase()

        val sLanguage: String
        sLanguage = when {
            language.contains("fr") -> "frFR"
            language.contains("ru") -> "ruRU"
            language.contains("pt") -> "ptBR"
            language.contains("ko") -> "koKR"
            else -> "enUS"
        }

        val cards = cache.load(io.williamwebb.hearthstone.parser.CardDb.Companion.KEY_CARDS + sLanguage)
        if (cards == null) {
            refreshCards(sLanguage).blockingGet()
        } else {
            storeCards(cards)
        }
    }

    private fun storeCards(cards: String) {
        synchronized(io.williamwebb.hearthstone.parser.CardDb.Companion.lock) {
            val list = Gson().fromJson<List<Card>>(cards, object : TypeToken<ArrayList<Card>>() {}.type)

            Collections.sort(list) { a, b -> a.id.compareTo(b.id) }
            sCardList = list
            cardMap = ConcurrentHashMap()

            for (c in list) {
                if (c.dbfId != Card.UNKNOWN.dbfId) {
                    cardMap.put(c.dbfId, c)
                } else {
                    Logger.e("Card Invalid: " + c)
                }
            }
            isReady = true
        }
    }

    fun getCards() = sCardList ?: ArrayList()

    fun getCard(key: String?): Card {
        synchronized(io.williamwebb.hearthstone.parser.CardDb.Companion.lock) {
            if (sCardList == null) {
                /**
                 * can happen the very first launch
                 */
                return Card.UNKNOWN
            }
            val index = Collections.binarySearch(sCardList, key as String)
            return if (index < 0) {
                Card.UNKNOWN
            } else {
                sCardList!![index]
            }
        }
    }

    fun getCardDBF(key: Int?) = cardMap[key] ?: Card.UNKNOWN

    private fun refreshCards(locale: String): Single<String> {
        return Single.fromCallable<String> {
            val endpoint = "https://api.hearthstonejson.com/v1/latest/$locale/cards.json"
            Logger.d("refreshingCards " + endpoint)
            val request = Request.Builder().url(endpoint).get().build()
            val response = OkHttpClient().newCall(request).execute()
            return@fromCallable response.takeIf { it.isSuccessful }?.body()?.string()
        }
        .doAfterSuccess {
            cache.save(io.williamwebb.hearthstone.parser.CardDb.Companion.KEY_CARDS + locale, it)
            storeCards(it)
        }
        .subscribeOn(Schedulers.io())
    }

    fun checkClassIndex(deck: Deck) {
        for (cardId in deck.cards.keys) {
            val card = getCard(cardId)
            val ci = Card.playerClassToClassIndex(card.playerClass)
            if (ci >= 0 && ci < Card.CLASS_INDEX_NEUTRAL) {
                if (deck.classIndex != ci) {
                    Logger.e("inconsistent class index, force to" + Card.classIndexToPlayerClass(ci))
                    deck.classIndex = ci
                }
                return
            }
        }
    }

    companion object {
        const val BOOK = "global"
        private val KEY_CARDS = "cards"
        private val lock = Any()
    }
}
