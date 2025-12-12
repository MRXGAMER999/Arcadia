# ==================== General Settings ====================
# Use R8 full mode for better shrinkage (optional, but recommended)
# (Enabled by default in recent AGP or via gradle.properties)

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Preserve common attributes for libraries (e.g., Annotations, Signatures for generics)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ==================== Kotlin ====================
-dontwarn kotlin.**
-dontwarn kotlinx.**
-keep class kotlin.reflect.** { *; }

# ==================== Android Components ====================
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View

# ==================== Jetpack Compose ====================
-keep class androidx.compose.ui.platform.WrappedComposition { *; }

# ==================== Retrofit & OkHttp ====================
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn okhttp3.internal.platform.**
-dontwarn okio.**

# ==================== Gson (Required for Appwrite) ====================
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# ==================== Appwrite ====================
# Appwrite uses Gson for serialization. Keep all Appwrite classes to be safe.
-keep class io.appwrite.** { *; }
-dontwarn io.appwrite.**

# ==================== Kotlin Serialization ====================
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serializer() on companion objects
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# ==================== Room ====================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ==================== Koin ====================
-keep class org.koin.** { *; }
-keep class org.koin.core.** { *; }
-dontwarn org.koin.**

# ==================== Coil ====================
-dontwarn coil.**

# ==================== Gemini / Google AI ====================
-dontwarn com.google.ai.client.generativeai.**

# ==================== OneSignal ====================
-dontwarn com.onesignal.**
-keep class com.onesignal.** { *; }

# ==================== Data Models (DTOs & Domain) ====================
-keep class com.example.arcadia.data.remote.dto.** { *; }
-keep class com.example.arcadia.domain.model.** { *; }

# Keep Mappers if they are used reflectively (unlikely but safe)
-keep class com.example.arcadia.data.mapper.** { *; }

# ==================== ViewModels ====================
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ==================== Miscellaneous ====================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
