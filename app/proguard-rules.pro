# Keep MiUnlockTool models, crypto, and HyperIsland classes
-keep class com.xboyrex.miunlocktool.** { *; }
-keep class io.github.d4viddf.hyperisland_kit.** { *; }

-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

-allowaccessmodification
-repackageclasses
-optimizationpasses 5

# Release logs are not user-visible
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
