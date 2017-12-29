package net.mbonnin.arcanetracker.parser.logreader

import io.williamwebb.hearthstone.parser.logreader.LogReader
import org.apache.commons.io.IOUtils
import org.apache.commons.io.input.Tailer
import org.apache.commons.io.input.TailerListener

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.ArrayList
import java.util.concurrent.Executors

import log.Logger
import java.nio.charset.Charset

/**
 * Created by martin on 10/17/16.
 */

class FileLogReader(path: String, entryPointsChoices: List<String>) : LogReader(path, entryPointsChoices) {

    private var tailer: Tailer? = null

    override fun readEntireFile(): List<String> {
        return try {
            IOUtils.readLines(FileInputStream(File(path)), Charset.forName("UTF8"))
        } catch (e: IOException) {
            ArrayList()
        }
    }

    override fun onStart() {
        tailer = Tailer(File(path), object : TailerListener {
            override fun init(tailer: Tailer) {}
            override fun fileNotFound() {}
            override fun fileRotated() {}

            override fun handle(line: String) {
                post(line)
            }

            override fun handle(ex: Exception) {
                Logger.e(ex, "Tailer encountered an Error!")
            }
        })
        Executors.newSingleThreadExecutor().execute(tailer)
    }

    override fun onStop() {
        tailer?.stop()
    }

}
