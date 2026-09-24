-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class ir.g1z4.controlpro.data.db.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn javax.annotation.**
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}
