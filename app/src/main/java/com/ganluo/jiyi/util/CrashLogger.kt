package com.ganluo.jiyi.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 极简崩溃日志:未捕获异常写 filesDir/crash-last.txt,同时 logcat tag=CRASHLOG */
object CrashLogger {
    private const val TAG = "CRASHLOG"
    private const val FILE_NAME = "crash-last.txt"

    fun install(context: Context) {
        val app = context.applicationContext
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching { write(app, t, e) }
            runCatching { Log.e(TAG, "uncaught", e) }
            prev?.uncaughtException(t, e)
        }
    }

    private fun write(ctx: Context, t: Thread, e: Throwable) {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val text = buildString {
            appendLine("time: $ts")
            appendLine("thread: ${t.name}")
            appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("abi: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("app: ${ctx.packageName}")
            appendLine("---- stack ----")
            appendLine(Log.getStackTraceString(e))
        }
        File(ctx.filesDir, FILE_NAME).writeText(text)
    }

    fun readLast(ctx: Context): String? = runCatching {
        val f = File(ctx.filesDir, FILE_NAME)
        if (f.exists() && f.length() > 0) f.readText() else null
    }.getOrNull()

    fun clear(ctx: Context) { runCatching { File(ctx.filesDir, FILE_NAME).delete() } }
}