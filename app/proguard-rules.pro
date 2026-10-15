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
