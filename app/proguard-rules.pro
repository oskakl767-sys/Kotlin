# ═══════════════════════════════════════════════════════════════════════
# ADVANCED OBFUSCATION RULES - MAXIMUM SECURITY
# ═══════════════════════════════════════════════════════════════════════

# Keep core Android classes
-keep class android.** { *; }
-keep class androidx.** { *; }

# Strip all debug info
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Optimization
-optimizationpasses 5
-allowaccessmodification

# Keep Socket.IO & OkHttp (needed for networking)
-keep class io.socket.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn io.socket.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep GSON models
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Protect Crypto Logic but obfuscate names
-keepclassmembers class com.mdm.agent.data.remote.CryptoManager {
    public <methods>;
}

# Remove Log calls for security
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
