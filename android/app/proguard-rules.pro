# Preserve Flutter engine classes
-keep class io.flutter.** { *; }
-keep class io.flutter.plugins.** { *; }

# Preserve app classes
-keep class io.github.benji377.timety.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep R class members
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Optimize aggressive inlining
-optimizationpasses 5
-dontusemixedcaseclassnames
