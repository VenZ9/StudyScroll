# StudyScroll ProGuard / R8 Rules

-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* *;
}

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# PDFBox Android
-dontwarn com.tom_roush.pdfbox.**
-keep class com.tom_roush.pdfbox.** { *; }
