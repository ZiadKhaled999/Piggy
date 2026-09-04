# ProGuard rules for Piggy Ledger
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.oryno.piggy_ledger.data.** { *; }
-keep class com.oryno.piggy_ledger.ai.** { *; }

# RevenueCat
-keep class com.revenuecat.purchases.** { *; }

# OkHttp & Ktor
-dontwarn java.lang.management.**
-dontwarn io.ktor.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class io.ktor.** { *; }

# Clerk
-dontwarn com.clerk.**
-keep class com.clerk.** { *; }

# CanHub Cropper
-dontwarn com.canhub.cropper.**
-keep class com.canhub.cropper.** { *; }

# Google Play Services & Firebase
-dontwarn com.google.android.gms.**
-dontwarn com.google.firebase.**

# AndroidX & Biometric
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keep class kotlinx.coroutines.** { *; }

# WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Services
-keep class com.oryno.piggy_ledger.service.** { *; }

