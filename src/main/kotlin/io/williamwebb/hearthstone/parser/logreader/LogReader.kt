package io.williamwebb.hearthstone.parser.logreader;

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import log.Logger
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * Created by williamwebb on 6/7/17.
 */

abstract class LogReader(protected val path: String, private val entryPointsChoices: List<String> = emptyList()) {

    private val publisher = PublishSubject.create<LogLine>()

    private var entryPoint by Delegates.notNull<LogLine>()
    private var previousLine = LogLine()

    data class LogLine(val line: String = "") {
        val time: Long = toEpoch(parseTime(line))
        val content: String = parseContent(line)
    }

    interface LineConsumer {
        fun onLine(logLine: LogLine)
    }

    private fun start() {
        val fileContents = readEntireFile()
        entryPoint = findEntryPoint(entryPointsChoices, fileContents)
        Logger.e("EntryPoint[" + path + "]: " + entryPoint.time + " : " + parseTime(entryPoint.line))

        fileContents.forEach { post(it) }
        onStart()
    }

    private fun stop() {
        onStop()
    }

    protected abstract fun readEntireFile(): List<String>
    protected abstract fun onStart()
    protected abstract fun onStop()

    protected fun post(line: String) {
        if (line.length < 18) return
        val logLine = LogLine(line)

        if (logLine.time <= entryPoint.time) {
            return
        }

        if (logLine.time < previousLine.time) {
            Logger.d("Time going backwards on $path ? ${logLine.time} < ${previousLine.time}")
        }

        publisher.onNext(logLine.apply { previousLine = this })
    }

    fun observe(): Observable<LogLine> = publisher
            .doOnSubscribe { start() }
            .doOnDispose({ stop() })

    companion object {
        fun <T: LogReader> observe(clazz: Class<T>, path: String, entryPointsChoices: List<String> = emptyList()): T {
            return clazz.getConstructor(String::class.java, List::class.java).newInstance(path, entryPointsChoices)
        }
    }
}

private val splitter = "[^0-9]".toRegex()
private val time_range = 2..17

internal fun parseTime(line: String) = line.takeIf { line.length > time_range.last }?.substring(time_range) ?: ""
internal fun parseContent(line: String) = line.takeIf { line.length > 19 }?.substring(19) ?: ""

internal fun toEpoch(timeString: String): Long {
    if (timeString.isEmpty()) return 0
    val (hours, minutes, seconds, micro) = timeString
            .split(splitter)
            .takeIf { it.size == 4 }
            ?: return 0

    return (TimeUnit.HOURS.toMicros(hours.toLong())) +
            TimeUnit.MINUTES.toMicros(minutes.toLong()) +
            TimeUnit.SECONDS.toMicros(seconds.toLong()) +
            micro.toLong()
}

internal fun findEntryPoint(choices: List<String>, input: List<String>): LogReader.LogLine {
    input.reversed().forEach { line ->
        choices.forEach { choice ->
            if (line.contains(choice)) {
                return LogReader.LogLine(line)
            }
        }
    }
    return if(input.isEmpty()) {
        LogReader.LogLine()
    } else {
        LogReader.LogLine(input.reversed()[0])
    }
}