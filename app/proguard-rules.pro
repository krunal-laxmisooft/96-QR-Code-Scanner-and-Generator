# Navigation type-safe routes are @Serializable; keep generated serializers
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.app.vv.infscan.** { *** Companion; }
-keepclasseswithmembers class com.app.vv.infscan.** { kotlinx.serialization.KSerializer serializer(...); }
