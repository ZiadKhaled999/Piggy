# ProGuard / R8 rules for Piggy Ledger
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# Kotlinx Serialization (used by data sync, AI models, RemoteConfig, MarketRates, Navigation routes)
-dontnote kotlinx.serialization.SerializationKt
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keep,allowobfuscation class <1>$$serializer {
    static <1>$$serializer INSTANCE;
}
-if @kotlinx.serialization.Serializable enum **
-keepclassmembers enum <1> {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve Navigation Compose type-safe route class names
-keepnames class com.oryno.piggy_ledger.ui.Screen**

# Room & WorkManager safety (supplements AAR consumer rules)
-keep class * extends androidx.room.RoomDatabase {
    void <init>();
}
-keepnames class * extends androidx.work.ListenableWorker
-keepclassmembers public class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Coroutines service providers
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Suppress warnings for optional JVM/server APIs in Ktor, OkHttp, Play Services, Firebase, Clerk, Biometric
-dontwarn java.lang.management.**
-dontwarn java.lang.ClassValue
-dontwarn org.slf4j.**
-dontwarn io.ktor.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.firebase.**
-dontwarn com.clerk.**
-dontwarn androidx.biometric.**
-dontwarn androidx.work.**
