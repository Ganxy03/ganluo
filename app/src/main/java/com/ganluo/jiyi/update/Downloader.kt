package com.ganluo.jiyi.update

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp 拉 APK,带进度回调。
 * 文件落到 filesDir/updates/ganluo-v{code}.apk
 */
@Singleton
class Downloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient
) {
    sealed interface State {
        data class Progress(val sofar: Long, val total: Long) : State
        data class Done(val file: File) : State
        data class Failed(val cause: Throwable) : State
    }

    fun download(url: String, code: Int): Flow<State> = flow {
        val dir = File(context.filesDir, "updates").apply { mkdirs() }
        val out = File(dir, "ganluo-v$code.apk")
        val req = Request.Builder().url(url).build()
        try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    emit(State.Failed(IllegalStateException("HTTP ${resp.code}")))
                    return@flow
                }
                val body = resp.body ?: run {
                    emit(State.Failed(IllegalStateException("空响应体")))
                    return@flow
                }
                val total = body.contentLength()
                out.outputStream().use { dst ->
                    body.byteStream().use { src ->
                        val buf = ByteArray(16 * 1024)
                        var read: Int
                        var sofar = 0L
                        while (src.read(buf).also { read = it } > 0) {
                            dst.write(buf, 0, read)
                            sofar += read
                            emit(State.Progress(sofar, total))
                        }
                    }
                }
                emit(State.Done(out))
            }
        } catch (e: Throwable) {
            emit(State.Failed(e))
        }
    }.flowOn(Dispatchers.IO)
}
