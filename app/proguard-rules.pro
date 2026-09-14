# ═══════════════════════════════════════════════════════════════════════════
#  Vizitor — آتیران ویزیتور | ProGuard Rules
#  Developed by Milano Technical Team, Milad Yaghoobi
# ═══════════════════════════════════════════════════════════════════════════
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# Retrofit
-keepattributes RuntimeVisibleAnnotations
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Gson DTOs
-keep class ir.atiran.vizitor.data.remote.dto.** { *; }
-keepclassmembers class ir.atiran.vizitor.data.remote.dto.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# ML Kit
-keep class com.google.mlkit.** { *; }
