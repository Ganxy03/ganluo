package com.ganluo.jiyi.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.ganluo.jiyi.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 崩溃 / 会话诊断日志(1.0.22 重写)
 *
 * 解决的问题:1.0.20 / 1.0.21 用户反馈「打开就闪退」,但拿不到任何堆栈,
 * 只能靠猜。这个类提供三层信息:
 *
 *  1. **Java 崩溃**:`Thread.setDefaultUncaughtExceptionHandler` 抓到后
 *     ①写本地 `crash-last.txt` ②**同步**上报到 `/api/crash`(进程马上要死了,
 *     必须用同步请求,不能丢给协程)③再交还给系统默认处理器(该崩还是崩)。
 *  2. **native 崩溃 / 被系统杀**:Java 层抓不到。用**面包屑**判断 ——
 *     每次启动往 `session.log` 写当前阶段,正常退出时写 `CLEAN`。
 *     下次启动如果发现上次没收尾,就把上次的面包屑当成「异常结束」上报,
 *     从最后一条面包屑就能知道死在哪个阶段。
 *  3. **本地自排查**:下次启动弹窗展示 `crash-last.txt`,用户可一键复制发给开发者。
 *
 * 上报失败不影响任何功能(超时 4 秒,全部 try/catch)。
 */
object CrashLogger {

    private const val TAG = "CRASHLOG"
    private const val CRASH_FILE = "crash-last.txt"
    private const val CRASH_SENT = "crash-last.sent"
    private const val SESSION_FILE = "session.log"
    private const val SESSION_PREV = "session-prev.log"
    private const val CLEAN_MARK = "CLEAN-EXIT"

    private fun ts(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

    private fun deviceInfo(ctx: Context): String =
        "${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) / " +
                "abi=${Build.SUPPORTED_ABIS.joinToString(",")} / pkg=${ctx.packageName}"

    // ===================== 安装全局兜底 =====================

    fun install(context: Context) {
        val app = context.applicationContext
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching { stage(app, "FATAL ${e.javaClass.name}: ${e.message?.take(120)}") }
            runCatching { writeCrash(app, t, e) }
            runCatching { Log.e(TAG, "uncaught exception", e) }
            // 进程马上要死:同步上报(最多 4 秒)
            runCatching { postCrash(app, buildJavaReport(app, t, e)) }
            runCatching { prev?.uncaughtException(t, e) }
        }
    }

    // ===================== 面包屑 / 会话 =====================

    /** 记录一个阶段。用于下次启动判断「上个进程死在哪」。 */
    fun stage(ctx: Context, name: String) {
        runCatching {
            val f = File(ctx.applicationContext.filesDir, SESSION_FILE)
            if (f.length() > 64 * 1024) f.writeText("")   // 防无限增长
            f.appendText("${ts()}  $name\n")
        }
    }

    /** 启动时调用一次:上一轮没收尾 → 归档为「异常结束」,然后开新的一轮 */
    fun beginSession(ctx: Context) {
        val app = ctx.applicationContext
        runCatching {
            val cur = File(app.filesDir, SESSION_FILE)
            if (cur.exists() && cur.length() > 0) {
                val text = cur.readText()
                if (!text.trimEnd().endsWith(CLEAN_MARK)) {
                    // 上次进程没有正常收尾:native 崩溃 / 被系统杀 / ANR
                    runCatching {
                        File(app.filesDir, SESSION_PREV).writeText(text.takeLast(32 * 1024))
                    }
                }
            }
            cur.writeText("")
            stage(app, "app.onCreate v${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})")
        }
    }

    /** 正常离开(Activity finish)时调用,标记本轮收尾 */
    fun endSession(ctx: Context) {
        runCatching {
            val f = File(ctx.applicationContext.filesDir, SESSION_FILE)
            if (f.exists()) f.appendText("${ts()}  $CLEAN_MARK\n")
        }
    }

    // ===================== 上报 =====================

    /**
     * 启动后异步调用:
     *  - 有「上一轮没收尾」的面包屑 → 上报为 session 异常
     *  - 有未上报过的 Java 崩溃 → 补报
     */
    fun uploadPending(ctx: Context) {
        val app = ctx.applicationContext
        runCatching {
            val prev = File(app.filesDir, SESSION_PREV)
            if (prev.exists() && prev.length() > 0) {
                val crumbs = prev.readText()
                if (postCrash(app, buildSessionReport(app, crumbs))) prev.delete()
            }
        }
        runCatching {
            val crash = File(app.filesDir, CRASH_FILE)
            val sent = File(app.filesDir, CRASH_SENT)
            if (crash.exists() && crash.length() > 0 && !sent.exists()) {
                if (postCrash(app, buildRawReport(app, crash.readText()))) sent.writeText(ts())
            }
        }
    }

    private fun jsonEscape(s: String): String {
        val sb = StringBuilder(maxOf(16, s.length + 16))
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c < ' ') sb.append(' ') else sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun buildJavaReport(ctx: Context, t: Thread, e: Throwable): String {
        val stack = Log.getStackTraceString(e).take(16 * 1024)
        return """
            {"type":"java","time":"${ts()}","device":"${jsonEscape(deviceInfo(ctx))}",
             "app_version":"${BuildConfig.VERSION_NAME}","app_code":${BuildConfig.VERSION_CODE},
             "thread":"${jsonEscape(t.name)}","stack":"${jsonEscape(stack)}",
             "session":"${jsonEscape(readSessionTail(ctx))}"}
        """.trimIndent().replace("\n", "")
    }

    private fun buildSessionReport(ctx: Context, crumbs: String): String = """
            {"type":"session","time":"${ts()}","device":"${jsonEscape(deviceInfo(ctx))}",
             "app_version":"${BuildConfig.VERSION_NAME}","app_code":${BuildConfig.VERSION_CODE},
             "thread":"-","stack":"进程异常结束(无 Java 堆栈,可能是 native 崩溃或被系统杀)",
             "session":"${jsonEscape(crumbs.take(16 * 1024))}"}
        """.trimIndent().replace("\n", "")

    private fun buildRawReport(ctx: Context, raw: String): String = """
            {"type":"java-retry","time":"${ts()}","device":"${jsonEscape(deviceInfo(ctx))}",
             "app_version":"${BuildConfig.VERSION_NAME}","app_code":${BuildConfig.VERSION_CODE},
             "thread":"-","stack":"${jsonEscape(raw.take(16 * 1024))}","session":""}
        """.trimIndent().replace("\n", "")

    /** 同步 POST,返回是否 2xx */
    private fun postCrash(ctx: Context, json: String): Boolean = runCatching {
        val url = URL(BuildConfig.API_BASE_URL.trimEnd('/') + "/api/crash")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 4000
            readTimeout = 4000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        conn.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        conn.disconnect()
        Log.i(TAG, "crash report posted, http=$code")
        code in 200..299
    }.getOrElse {
        Log.w(TAG, "crash report post failed: ${it.message}")
        false
    }

    // ===================== 本地文件 =====================

    private fun writeCrash(ctx: Context, t: Thread, e: Throwable) {
        val text = buildString {
            appendLine("time: ${ts()}")
            appendLine("thread: ${t.name}")
            appendLine("device: ${deviceInfo(ctx)}")
            appendLine("app: v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("---- session ----")
            appendLine(readSessionTail(ctx))
            appendLine("---- stack ----")
            appendLine(Log.getStackTraceString(e))
        }
        File(ctx.filesDir, CRASH_FILE).writeText(text)
        File(ctx.filesDir, CRASH_SENT).delete()
    }

    private fun readSessionTail(ctx: Context): String =
        runCatching {
            val f = File(ctx.applicationContext.filesDir, SESSION_FILE)
            if (f.exists() && f.length() > 0) f.readText().takeLast(8 * 1024) else ""
        }.getOrDefault("")

    /** 上次崩溃内容(给 UI 弹窗用) */
    fun readLast(ctx: Context): String? = runCatching {
        val f = File(ctx.applicationContext.filesDir, CRASH_FILE)
        if (f.exists() && f.length() > 0) f.readText() else null
    }.getOrNull()

    fun clear(ctx: Context) {
        runCatching { File(ctx.applicationContext.filesDir, CRASH_FILE).delete() }
        runCatching { File(ctx.applicationContext.filesDir, CRASH_SENT).delete() }
    }
}
