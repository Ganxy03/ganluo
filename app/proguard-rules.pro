# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }

# Moshi (反射路径,codegen 路径不需要)
-keepclassmembers class * {
    @com.squareup.moshi.JsonClass <fields>;
    @com.squareup.moshi.Json <fields>;
}
-keep class com.ganluo.jiyi.data.remote.dto.** { *; }

# Retrofit
-keepattributes Signature, Exceptions
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class retrofit2.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Compose
-keep class androidx.compose.** { *; }

# ===== 高德 SDK (11.3.100) =====
# 反射/动态加载较多,但 -keep 所有类会保留死代码;只 keep 关键入口
-dontwarn com.amap.ams.gnss.GnssSoftLocator
-dontwarn net.jafama.FastMath
-dontwarn com.amap.**
-dontwarn com.autonavi.**

# 地图核心 API(被我们直接 import)
-keep class com.amap.api.maps.** { *; }
-keep interface com.amap.api.maps.** { *; }

# 定位 SDK
-keep class com.amap.api.location.** { *; }

# 搜索 SDK(11.3.100 fat jar 自带,虽然 1.0.12 没直接用,可能被 aar 内部反射)
-keep class com.amap.api.services.** { *; }

# 公共基类 / 工具(避免 R8 砍掉被反射引用的)
-keep class com.autonavi.** { *; }
-keep class com.amap.api.**.core.** { *; }
