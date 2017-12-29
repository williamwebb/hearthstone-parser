package io.williamwebb.hearthstone.parser

import java.io.File
import java.io.PrintWriter

class FileCache(val path: String) : CardDb.Cache {
    override fun save(key: String, json: String?) {
        val writer = PrintWriter("$path/card_db_$key.txt", "UTF-8")
        writer.print(json)
        writer.close()
    }

    override fun load(key: String): String? {
        return try {
            val file = File("$path/card_db_$key.txt")
            if(!file.exists()) null
            else file.readText()
        } catch (e: Exception) {
            throw e
        }
    }
}