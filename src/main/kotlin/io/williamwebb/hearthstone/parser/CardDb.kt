package io.williamwebb.hearthstone.parser

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.williamwebb.hearthstone.parser.models.Card
import io.williamwebb.hearthstone.parser.models.Deck
import log.Logger
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by martin on 11/10/16.
 */

class CardDb @JvmOverloads constructor(private val cache: io.williamwebb.hearthstone.parser.CardDb.Cache, locale: Locale = Locale.getDefault()) {
    private var sCardList: List<Card> = emptyList()
    private var cardMap: Map<Int, Card> = HashMap()
    private var isReady: Boolean = false

    private val cardService = Retrofit.Builder()
            .baseUrl("https://api.hearthstonejson.com")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build().create(CardsService::class.java)

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

        val cards = cache.load(KEY_CARDS + "_" + sLanguage)
        if (cards == null) {
            refreshCards(sLanguage).blockingGet()
        } else {
            storeCards(cardsAdapter.fromJson(cards))
        }
    }

    private fun storeCards(cards: List<Card>) {
        synchronized(Companion.lock) {

            Collections.sort(cards) { a, b -> a.id.compareTo(b.id) }
            sCardList = cards
            cardMap = cards.filter { it.dbfId != Card.UNKNOWN.dbfId }.associateBy({it.dbfId}, {it})

            isReady = true
        }
    }

    fun getCards() = sCardList

    fun getCard(key: String?): Card {
        synchronized(Companion.lock) {
            val index = Collections.binarySearch(sCardList, key as String)
            return if (index < 0) {
                Card.UNKNOWN
            } else {
                sCardList[index]
            }
        }
    }

    fun getCardDBF(key: Int?) = cardMap[key] ?: Card.UNKNOWN

    private fun refreshCards(locale: String): Single<List<Card>> {
        println(cardService)
        println(locale)
        return cardService.cards(locale)
        .doAfterSuccess {
            cache.save(KEY_CARDS + "_" + locale, cardsAdapter.toJson(it))
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

    interface CardsService {
        @GET("v1/latest/{locale}/cards.json")
        fun cards(@Path("locale") locale: String): Single<List<Card>>
    }

    companion object {
        const val BOOK = "global"
        private val KEY_CARDS = "cards"
        private val lock = Any()
        private val moshi = Moshi.Builder().build()
        private val cardsAdapter = moshi.adapter<List<Card>>(Types.newParameterizedType(List::class.java, Card::class.java))
    }
}
