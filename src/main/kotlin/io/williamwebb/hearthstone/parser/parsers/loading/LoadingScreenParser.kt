package io.williamwebb.hearthstone.parser.parsers.loading

import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject.create
import io.williamwebb.hearthstone.parser.logreader.LogReader
import log.Logger

/**
 * Created by martin on 11/7/16.
 */

class LoadingScreenParser(private val logReader: LogReader){

    private val eventPublisher = create<Event>()
    private val rawPublisher = create<LogReader.LogLine>()

    sealed class Event {
        data class MODE_CHANGED(val mode: Int) : Event()
    }

    interface Listener {
        fun modeChanged(newMode: Int)
    }

    fun EVENTS() = eventPublisher
    fun RAW() = rawPublisher

    fun start(): Disposable {
        return logReader.observe().subscribe ({
            onLine(it)
        }, Logger::e)
    }

    private fun onLine(logLine: LogReader.LogLine) {
        rawPublisher.onNext(logLine)

        val line = logLine.content

        val matcher = pattern.matcher(line)
        if (matcher.matches()) {
//            val prevMode = matcher.group(1)
            val currMode = matcher.group(2)

            if (currMode == "GAMEPLAY") {
                return
            }

            val newMode = when (currMode) {
                "DRAFT" -> MODE_ARENA
                "TOURNAMENT" -> MODE_PLAY
                else -> MODE_OTHER
            }

            eventPublisher.onNext(Event.MODE_CHANGED(newMode))
        }
    }

    companion object {
        val MODE_PLAY = 0
        val MODE_ARENA = 1
        val MODE_OTHER = 2
        private val pattern = "LoadingScreen.OnSceneLoaded\\(\\) - prevMode=(.*) currMode=(.*)".toPattern()
    }

}
