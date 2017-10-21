package io.williamwebb.hearthstone.parser.models

import log.Logger
import java.util.regex.Pattern

/**
 * Created by martin on 10/17/16.
 */
class Card(@JvmField val name: String,
           @JvmField val playerClass: String,
           @JvmField val cost: Int,
           @JvmField val id: String,
           @JvmField val dbfId: Int,
           @JvmField val rarity: String,
           @JvmField val type: String,
           @JvmField val text: String,
           @JvmField val race: String,
           @JvmField val set: String,
           @JvmField val collectible: Boolean) : Comparable<String> {


    override fun compareTo(other: String): Int {
        return id.compareTo(other)
    }

    override fun toString(): String {
        return "$name($id)"
    }

    companion object {
        @JvmField val classNameList = arrayOf("Warrior", "Shaman", "Rogue", "Paladin", "Hunter", "Druid", "Warlock", "Mage", "Priest")

        @JvmField val RARITY_LEGENDARY = "LEGENDARY"

        @JvmField val TYPE_HERO = "HERO"
        @JvmField val TYPE_UNKNOWN = "TYPE_UNKNOWN"
        @JvmField val TYPE_SPELL = "SPELL"
        @JvmField val TYPE_MINION = "MINION"
        @JvmField val TYPE_WEAPON = "WEAPON"
        @JvmField val TYPE_HERO_POWER = "HERO_POWER"
        @JvmField val TYPE_ENCHANTMENT = "ENCHANTMENT"

        @JvmField val RACE_MECHANICAL = "MECHANICAL"
        @JvmField val RACE_MURLOC = "MURLOC"
        @JvmField val RACE_DEMON = "DEMON"
        @JvmField val RACE_BEAST = "BEAST"
        @JvmField val RACE_TOTEM = "TOTEM"
        @JvmField val RACE_PIRATE = "PIRATE"
        @JvmField val RACE_DRAGON = "DRAGON"

        @JvmField val CLASS_INDEX_WARRIOR = 0
        @JvmField val CLASS_INDEX_SHAMAN = 1
        @JvmField val CLASS_INDEX_ROGUE = 2
        @JvmField val CLASS_INDEX_PALADIN = 3
        @JvmField val CLASS_INDEX_HUNTER = 4
        @JvmField val CLASS_INDEX_DRUID = 5
        @JvmField val CLASS_INDEX_WARLOCK = 6
        @JvmField val CLASS_INDEX_MAGE = 7
        @JvmField val CLASS_INDEX_PRIEST = 8
        @JvmField val CLASS_INDEX_NEUTRAL = 9

        @JvmField val CLASS_WARRIOR = "WARRIOR"
        @JvmField val CLASS_SHAMAN = "SHAMAN"
        @JvmField val CLASS_ROGUE = "ROGUE"
        @JvmField val CLASS_PALADIN = "PALADIN"
        @JvmField val CLASS_HUNTER = "HUNTER"
        @JvmField val CLASS_DRUID = "DRUID"
        @JvmField val CLASS_WARLOCK = "WARLOCK"
        @JvmField val CLASS_MAGE = "MAGE"
        @JvmField val CLASS_PRIEST = "PRIEST"
        @JvmField val CLASS_NEUTRAL = "NEUTRAL"

        @JvmField val ID_COINe = "GAME_005e"
        @JvmField val ID_COIN = "GAME_005"
        @JvmField val ID_GANG_UP = "BRM_007"
        @JvmField val ID_BENEATH_THE_GROUNDS = "AT_035"
        @JvmField val ID_AMBUSHTOKEN = "AT_035t"
        @JvmField val ID_IRON_JUGGERNAUT = "GVG_056"
        @JvmField val ID_BURROWING_MINE_TOKEN = "GVG_056t"
        @JvmField val ID_RECYCLE = "GVG_031"
        @JvmField val MANIC_SOULCASTER = "CFM_660"
        @JvmField val FORGOTTEN_TORCH = "LOE_002"
        @JvmField val ROARING_TORCH = "LOE_002t"
        @JvmField val CURSE_OF_RAFAAM = "LOE_007"
        @JvmField val CURSED = "LOE_007t"
        @JvmField val ANCIENT_SHADE = "LOE_110"
        @JvmField val ANCIENT_CURSE = "LOE_110t"
        @JvmField val EXCAVATED_EVIL = "LOE_111"
        @JvmField val ELISE = "LOE_079"
        @JvmField val MAP_TO_THE_GOLDEN_MONKEY = "LOE_019t"
        @JvmField val GOLDEN_MONKEY = "LOE_019t2"
        @JvmField val DOOMCALLER = "OG_255"
        @JvmField val CTHUN = "OG_280"
        @JvmField val JADE_IDOL = "CFM_602"
        @JvmField val WHITE_EYES = "CFM_324"
        @JvmField val STORM_GUARDIAN = "CFM_324t"
        @JvmField val FLAME_ELEMENTAL = "UNG_809t1"
        @JvmField val FLAME_GEYSER = "UNG_018"
        @JvmField val PYROS2 = "UNG_027"
        @JvmField val PYROS6 = "UNG_027t2"
        @JvmField val PYROS10 = "UNG_027t4"
        @JvmField val STEAM_SURGER = "UNG_021"
        @JvmField val RAZORPETAL_LASHER = "UNG_058"
        @JvmField val RAZORPETAL = "UNG_057t1"
        @JvmField val RAZORPETAL_VOLLEY = "UNG_057"
        @JvmField val DEADLY_FORK = "KAR_094"
        @JvmField val SHARP_FORK = "KAR_094a"
        @JvmField val SHADOWCASTER = "OG_291"
        @JvmField val FIREFLY = "UNG_809"
        @JvmField val IGNEOUS_ELEMENTAL = "UNG_845"
        @JvmField val BURGLY_BULLY = "CFM_669"
        @JvmField val BANANA = "EX1_014t"
        @JvmField val KING_MUKLA = "EX1_014"
        @JvmField val MUKLA_TYRANT = "OG_122"
        @JvmField val RHONIN = "AT_009"
        @JvmField val ARCANE_MISSILE = "EX1_277"
        @JvmField val JUNGLE_GIANTS = "UNG_116"
        @JvmField val BARNABUS = "UNG_116t"
        @JvmField val THE_MARSH_QUEEN = "UNG_920"
        @JvmField val QUEEN_CARNASSA = "UNG_920t1"
        @JvmField val OPEN_THE_WAYGATE = "UNG_028"
        @JvmField val TIME_WARP = "UNG_028t"
        @JvmField val THE_LAST_KALEIDOSAUR = "UNG_954"
        @JvmField val GALVADON = "UNG_954t1"
        @JvmField val AWAKEN_THE_MAKERS = "UNG_940"
        @JvmField val AMARA = "UNG_940t8"
        @JvmField val CAVERNS_BELOW = "UNG_067"
        @JvmField val CRYSTAL_CORE = "UNG_067t1"
        @JvmField val UNITE_THE_MURLOCS = "UNG_942"
        @JvmField val MEGAFIN = "UNG_942t"
        @JvmField val LAKKARI_SACRIFICE = "UNG_829"
        @JvmField val NETHER_PORTAL = "UNG_829t1"
        @JvmField val FIRE_PLUME = "UNG_934"
        @JvmField val SULFURAS = "UNG_934t1"
        @JvmField val BEAR_TRAP = "AT_060"
        @JvmField val CAT_TRICK = "KAR_004"
        @JvmField val DART_TRAP = "LOE_021"
        @JvmField val EXPLOSIVE_TRAP = "EX1_610"
        @JvmField val FREEZING_TRAP = "EX1_611"
        @JvmField val HIDDEN_CACHE = "CFM_026"
        @JvmField val MISDIRECTION = "EX1_533"
        @JvmField val SNAKE_TRAP = "EX1_554"
        @JvmField val SNIPE = "EX1_609"
        @JvmField val COUNTERSPELL = "EX1_287"
        @JvmField val DUPLICATE = "FP1_018"
        @JvmField val MIRROR_ENTITY = "EX1_294"
        @JvmField val MANA_BIND = "UNG_024"
        @JvmField val ICE_BLOCK = "EX1_295"
        @JvmField val EFFIGY = "AT_002"
        @JvmField val ICE_BARRIER = "EX1_289"
        @JvmField val POTION_OF_POLYMORPH = "CFM_620"
        @JvmField val SPELL_BENDER = "tt_010"
        @JvmField val VAPORIZE = "EX1_594"
        @JvmField val COMPETITIVE_SPIRIT = "AT_073"
        @JvmField val AVENGE = "FP1_020"
        @JvmField val EYE_FOR_EYE = "EX1_132"
        @JvmField val GETAWAY_KOD = "CFM_800"
        @JvmField val NOBLE_SACRIFIC = "EX1_130"
        @JvmField val REDEMPTION = "EX1_136"
        @JvmField val REPENTANCE = "EX1_379"
        @JvmField val RAPTOR_HATCHLING = "UNG_914"
        @JvmField val RAPTOR_PATRIARCH = "UNG_914t1"
        @JvmField val DIREHORN_HATCHLING = "UNG_957"
        @JvmField val DIREHORN_MATRIARCH = "UNG_957t1"

        @JvmField val UNKNOWN = Card("?", "?", -1, "?", -1, "?", TYPE_UNKNOWN, "?", "?", "?", false)

        @JvmStatic
        fun heroIdToClassIndex(heroId: String): Int {

            val pattern = Pattern.compile("hero_([0-9]*)[a-zA-Z]*", Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(heroId)
            if (matcher.matches()) {
                try {
                    var i = Integer.parseInt(matcher.group(1))
                    i--

                    if (i in 0..8) {
                        return i
                    }

                } catch (e: Exception) {
                    Logger.e("wrong heroId" + heroId)
                }
            }
            return -1
        }

        @JvmStatic
        fun classIndexToPlayerClass(classIndex: Int): String {
            return when (classIndex) {
                CLASS_INDEX_WARRIOR -> "WARRIOR"
                CLASS_INDEX_ROGUE -> "ROGUE"
                CLASS_INDEX_SHAMAN -> "SHAMAN"
                CLASS_INDEX_PALADIN -> "PALADIN"
                CLASS_INDEX_HUNTER -> "HUNTER"
                CLASS_INDEX_DRUID -> "DRUID"
                CLASS_INDEX_WARLOCK -> "WARLOCK"
                CLASS_INDEX_MAGE -> "MAGE"
                CLASS_INDEX_PRIEST -> "PRIEST"
                else -> "NEUTRAL"
            }
        }

        @JvmStatic
        fun classIndexToHeroId(classIndex: Int): String {
            return String.format("hero_%02d", classIndex + 1)
        }

        @JvmStatic
        fun playerClassToClassIndex(playerClass: String): Int {
            return (0 until CLASS_INDEX_NEUTRAL).firstOrNull { classIndexToPlayerClass(it) == playerClass } ?: -1
        }

        fun isCollectible(cardDb: io.williamwebb.hearthstone.parser.CardDb, cardId: String): Boolean {
            val card = cardDb.getCard(cardId)
            if (!card.collectible) {
                return false
            }

            if (ID_COINe == cardId || ID_COIN == cardId) {
                return false
            }
            return true
        }
    }

}
