# Runcheck ProGuard Rules

# Strip verbose/debug/info log calls from release builds, keep warn/error for diagnostics.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.runcheck.data.device.DeviceProfile { *; }

# Glance widgets (manifest-declared components)
-keep class * extends androidx.glance.appwidget.GlanceAppWidget
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver

# NDT7 speed test models and callbacks used across the app/library boundary
-keep class net.measurementlab.ndt7.android.NdtTest { *; }
-keep class net.measurementlab.ndt7.android.models.ClientResponse { *; }
-keep class net.measurementlab.ndt7.android.models.Measurement { *; }
-keep class net.measurementlab.ndt7.android.utils.DataConverter { *; }
