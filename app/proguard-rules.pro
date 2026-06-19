# BestieAYG ProGuard / R8 rules.
#
# Goal: enable R8 on release builds so class/method names are obfuscated
# and dead code is stripped, while keeping everything that depends on
# reflection (Retrofit, Gson, Room, Compose, Firebase, Kotlin coroutines).
#
# Most third-party libs ship their own consumer-proguard-rules.pro, so
# this file only adds keeps for OUR reflective surface plus a few
# defensive rules for common pitfalls.

# ---- Stack-trace readability for crash reports --------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotation metadata that libraries use at runtime (Retrofit, Room,
# Firebase, Compose, Gson all rely on this).
-keepattributes *Annotation*,Signature,Exceptions,InnerClasses,EnclosingMethod

# ---- Retrofit / OkHttp --------------------------------------------
# Retrofit creates a dynamic proxy for our service interface; methods must
# survive obfuscation so the @GET / @Query annotations are still reachable.
-keep,allowobfuscation interface com.bayg.data.remote.WeatherApiService

# Retrofit's own consumer rules cover most of the rest; this is belt-and-braces.
-keepclasseswithmembers,allowobfuscation class * {
    @retrofit2.http.* <methods>;
}

# OkHttp uses platform classes that R8 sometimes can't resolve on older SDKs.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---- Gson model classes -------------------------------------------
# Response DTOs are populated reflectively by Gson. Fields would otherwise
# be renamed and Gson would write into ghost fields.
-keep class com.bayg.data.remote.model.** { *; }
-keep class com.bayg.data.remote.model.**$* { *; }

# Generic Gson safety net for any other @SerializedName usage.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ---- Room ---------------------------------------------------------
# Room ships its own consumer rules, but keeping the entity classes
# explicitly protects against Gson + Room sharing the same DTOs.
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keep class com.bayg.services.storage.entities.** { *; }

# ---- SQLCipher ----------------------------------------------------
# SQLCipher's JNI layer is reached by reflection from the AAR. Keep the
# whole `net.zetetic.database.sqlcipher` package so R8 does not rename
# the entry points that the native library expects.
-keep class net.zetetic.database.sqlcipher.** { *; }
-keep class net.zetetic.database.** { *; }
-dontwarn net.zetetic.database.**

# ---- Firebase -----------------------------------------------------
# Firebase SDKs ship consumer rules; these two lines silence false-positive
# warnings about reflective access that the SDK handles internally.
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ---- Kotlinx coroutines -------------------------------------------
# coroutines-core ships consumer rules; the warning suppression keeps R8
# quiet about internal classes that are intentionally not on the runtime
# classpath in production builds.
-dontwarn kotlinx.coroutines.debug.**

# ---- Compose ------------------------------------------------------
# Compose runtime has its own rules; nothing extra needed.

# ---- BuildConfig --------------------------------------------------
# Keep BuildConfig so the few BuildConfig.DEBUG checks in our code (e.g.
# WeatherRepository's HTTP logging level) survive shrinking.
-keep class com.bayg.BuildConfig { *; }
