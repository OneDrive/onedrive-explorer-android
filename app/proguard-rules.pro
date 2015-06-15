### okhttp ###
-dontwarn com.squareup.okhttp.**
-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }

### retrofit ###
-dontwarn retrofit.**
-keep class retrofit.** { *; }

### gson ###
-keepattributes *Annotation*
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault

### okio ###
-dontwarn okio.**

### joda time ###
-dontwarn org.joda.time.**