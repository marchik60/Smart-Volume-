package com.smartvolume.pro.data

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: String,
    val level: String,
    val tag: String,
    val message: String
)

object AppLog {
    private const val MAX_ENTRIES = 400
    val entries = mutableStateListOf<LogEntry>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun i(tag: String, msg: String) = add("INFO", tag, msg)
    fun w(tag: String, msg: String) = add("WARN", tag, msg)
    fun e(tag: String, msg: String, tr: Throwable? = null) {
        val fullMsg = if (tr != null) "$msg | ${tr.javaClass.simpleName}: ${tr.message}" else msg
        add("ERR", tag, fullMsg)
    }

    private fun add(level: String, tag: String, msg: String) {
        val entry = LogEntry(timeFormat.format(Date()), level, tag, msg)
        if (entries.size >= MAX_ENTRIES) {
            entries.removeAt(0)
        }
        entries.add(entry)
    }

    fun hasErrors(): Boolean = entries.any { it.level == "ERR" || it.level == "WARN" }

    fun exportText(hwInfo: String): String {
        val sb = StringBuilder()
        sb.appendLine("=== SMART-VOLUME PRO DIAGNOSTIC REPORT ===")
        sb.appendLine(hwInfo)
        sb.appendLine("==========================================")
        entries.forEach {
            sb.appendLine("[${it.timestamp}] [${it.level}] ${it.tag}: ${it.message}")
        }
        return sb.toString()
    }
}
