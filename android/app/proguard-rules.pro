# 村医AI ProGuard 规则

# Hilt
-keepclasseswithmembers class * {
    @dagger.hilt.android.* <methods>;
}
-keepclasseswithmembers class * {
    @dagger.hilt.* <fields>;
}

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable classes
-keep,includedescriptorclasses class com.cunyi.ai.model.**$$serializer { *; }
-keepclassmembers class com.cunyi.ai.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.cunyi.ai.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# JNI
-keepclasseswithmembers class * {
    native <methods>;
}
-keepclassmembers,allowobfuscation class * {
    @com.google.jni.* <fields>;
}

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep Compose
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
