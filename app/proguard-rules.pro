# DevicePulse ProGuard Rules

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
-keep class com.devicepulse.data.device.DeviceProfile { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel

# Google Play Billing
-keep class com.android.vending.billing.** { *; }

# Glance widgets
-keep class * extends androidx.glance.appwidget.GlanceAppWidget
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver

# Coroutines
-dontwarn kotlinx.coroutines.**

# Domain model classes (used by Room/Gson)
-keep class com.devicepulse.data.db.entity.** { *; }
-keep class com.devicepulse.domain.model.** { *; }
