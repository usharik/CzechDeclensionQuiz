# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep line numbers for readable Crashlytics stack traces.
-keepattributes SourceFile,LineNumberTable

# --- Gson rules (see https://github.com/google/gson/blob/main/Troubleshooting.md#r8) ---

# Gson generic type signatures (e.g. TypeToken<HashMap<String, Int>>) rely on
# generic signature attributes being kept.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken

# Gson reads the bundled noun, adjective, and verb dictionaries by reflecting
# over these classes. Keep their constructors and JSON field names in release.
-keep class com.usharik.database.WordInfo { *; }
-keep class com.usharik.database.AdjectiveInfo { *; }
-keep class com.usharik.database.VerbInfo { *; }

# Room entities are accessed via generated code but keep them defensively
# since they are also passed through/around reflection-sensitive paths.
-keep class com.usharik.database.dao.*Entity { *; }
