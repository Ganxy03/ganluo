package com.ganluo.jiyi.data.remote

import com.ganluo.jiyi.data.remote.dto.Envelope
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.lang.reflect.Type

/**
 * 把后端统一响应 { code, message, data } 自动解包成 data,
 * 失败时抛 ApiException(code, message),业务层用 try/catch 处理。
 */
class UnwrapConverterFactory(
    private val moshi: Moshi
) : Converter.Factory() {

    private val delegate: MoshiConverterFactory = MoshiConverterFactory.create(moshi)

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> = EnvelopeConverter(moshi, type)

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody> {
        val c = delegate.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)
        return c ?: throw IllegalStateException("Moshi request body converter not available")
    }

    private class EnvelopeConverter(
        private val moshi: Moshi,
        private val type: Type
    ) : Converter<ResponseBody, Any> {
        override fun convert(value: ResponseBody): Any {
            val rawJson = value.string()
            val envelopeType = Types.newParameterizedType(Envelope::class.java, type)
            val envAdapter = moshi.adapter<Envelope<Any>>(envelopeType)
            val env = envAdapter.fromJson(rawJson)
                ?: throw ApiException(code = -1, message = "空响应")

            if (env.code != 0) {
                throw ApiException(code = env.code, message = env.message ?: "请求失败")
            }

            val data = env.data
            if (data == null) {
                // 响应里 data 为 null,期望的 type 是 Unit(Java Void) → 返回 0
                if (type == Unit::class.java) return Unit
                throw ApiException(code = -1, message = "data 为空")
            }

            // 把 data 直接当目标类型用 moshi 重新解一次(可能 data 是 Map/List/object,要看后端)
            return when (data) {
                is Map<*, *>, is List<*> -> {
                    val anyAdapter = moshi.adapter<Any>(type)
                    val json = moshi.adapter(Any::class.java).toJson(data)
                    anyAdapter.fromJson(json) ?: throw ApiException(-1, "反序列化失败")
                }
                else -> data
            }
        }
    }
}

class ApiException(
    val code: Int,
    override val message: String
) : RuntimeException(message)
