package io.williamwebb.hearthstone.parser.utils

import java.util.*

/**
 * Created by williamwebb on 6/21/17.
 */

object Utils {

    @JvmStatic
    fun cardMapTotal(map: Map<String, Int>) = map.keys.sumBy { cardMapGet(map, it) }

    @JvmStatic
    fun extractMethod(line: String): Array<String>? {
        val i = line.indexOf('-')
        if (i < 2 || i >= line.length - 2) {
            return null
        }

        if (line[i - 1] != ' ' || line[i + 1] != ' ') {
            return null
        }

        return arrayOf(line.substring(0, i - 1), line.substring(i + 2))
    }

    @JvmStatic
    fun cardMapGet(map: Map<String, Int>, key: String): Int {
        var a: Int? = map[key]
        if (a == null) {
            a = 0
        }
        return a
    }

    @JvmStatic
    fun cardMapAdd(map: MutableMap<String, Int>, key: String, diff: Int) {
        val a = cardMapGet(map, key)
        map.put(key, a + diff)
    }

    @JvmStatic
    fun cardMapDiff(a: Map<String, Int>, b: Map<String, Int>): Map<String, Int> {
        val map = HashMap<String, Int>()

        val set = HashSet(a.keys)
        set.addAll(b.keys)

        for (key in set) {
            val an = a[key]
            val bn = cardMapGet(b, key)

            if (an == null) {
                System.err.printf("key %s is not in a", key)
                continue
            }

            map.put(key, an - bn)
        }

        return map
    }
}