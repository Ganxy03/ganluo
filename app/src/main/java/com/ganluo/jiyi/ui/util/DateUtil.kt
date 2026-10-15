package com.ganluo.jiyi.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 对应原 utils/format.js 的 8 个工具,Kotlin 直搬。
 */
object DateUtil {
    private const val DATE_FMT = "yyyy-MM-dd"
    private const val TIME_FMT = "HH:mm"

    fun toDateStr(d: Date = Date()): String =
        SimpleDateFormat(DATE_FMT, Locale.US).format(d)

    fun toDateTimeStr(d: Date): String {
        val sdf = SimpleDateFormat("$DATE_FMT $TIME_FMT", Locale.US)
        return sdf.format(d)
    }

    fun shiftDate(dateStr: String, deltaDays: Int): String {
        val (y, m, d) = dateStr.split("-").map { it.toInt() }
        val cal = Calendar.getInstance().apply {
            set(y, m - 1, d)
            add(Calendar.DAY_OF_MONTH, deltaDays)
        }
        return toDateStr(cal.time)
    }

    fun friendlyDate(dateStr: String): String {
        val today = toDateStr()
        val yesterday = shiftDate(today, -1)
        val tomorrow = shiftDate(today, 1)
        when (dateStr) {
            today -> return "今天"
            yesterday -> return "昨天"
            tomorrow -> return "明天"
        }
        val (y, m, d) = dateStr.split("-").map { it.toInt() }
        val cal = Calendar.getInstance().apply { set(y, m - 1, d) }
        val weekday = arrayOf("日", "一", "二", "三", "四", "五", "六")[cal.get(Calendar.DAY_OF_WEEK) - 1]
        return "${m}月${d}日 周$weekday"
    }

    fun timeOf(datetime: String?): String {
        if (datetime.isNullOrBlank()) return ""
        val m = Regex("(\\d{2}):(\\d{2})").find(datetime) ?: return ""
        return m.value
    }

    fun isToday(dateStr: String): Boolean = dateStr == toDateStr()

    fun initialOf(name: String?): String {
        val trimmed = (name ?: "·").trim()
        return if (trimmed.isEmpty()) "·" else trimmed.substring(0, 1)
    }
}
