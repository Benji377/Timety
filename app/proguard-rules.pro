# Timety R8/ProGuard rules.
#
# The app deliberately avoids reflection-based serialization (backup/restore is hand-written
# org.json), so almost everything can be minified. Library-specific rules for Room, Compose,
# Glance and Coil ship as consumer rules inside those artifacts.

# Keep crash reports readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Coroutines debug metadata is stripped; silence notes about it.
-dontwarn kotlinx.coroutines.debug.**
