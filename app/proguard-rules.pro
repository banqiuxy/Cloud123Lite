# Cloud123 ProGuard 规则

# ---------- kotlinx.serialization ----------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.banqiu.thirdparty123pan.**$$serializer { *; }
-keepclassmembers class com.banqiu.thirdparty123pan.** {
    *** Companion;
}
-keepclasseswithmembers class com.banqiu.thirdparty123pan.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------- Retrofit ----------
-keepattributes Signature, Exceptions
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ---------- OkHttp ----------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# ---------- Coil ----------
-dontwarn coil3.**

# ---------- Room ----------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# ---------- Hilt ----------
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ---------- ZXing ----------
-dontwarn com.google.zxing.**

# ---------- Gson/其他 ----------
-dontwarn org.jetbrains.annotations.**

# ---------- 传输实体（Room + 反射） ----------
-keep class com.banqiu.thirdparty123pan.data.db.entity.** { *; }