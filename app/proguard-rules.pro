# Runcheck ProGuard Rules

# Strip Android logcat calls from release builds.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.runcheck.data.device.DeviceProfile { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel

# Google Play Billing
-keep class com.android.vending.billing.** { *; }

# Google AdMob
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Glance widgets
-keep class * extends androidx.glance.appwidget.GlanceAppWidget
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver

# Coroutines
-dontwarn kotlinx.coroutines.**

# OkHttp (used by NDT7 speed test library)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# NDT7 speed test
-keep class net.measurementlab.** { *; }

# Domain model classes (used by Room/Gson)
-keep class com.runcheck.data.db.entity.** { *; }
-keep class com.runcheck.domain.model.** { *; }
