# Runcheck ProGuard Rules
# R8 full mode is active (AGP 9.x default) — all reflection paths must be explicit.

# Strip verbose/debug/info log calls from release builds, keep warn/error for diagnostics.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# ── Room ─────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ── Gson ─────────────────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*

# DeviceProfile is serialized/deserialized with Gson via DeviceProfileRepositoryImpl
-keep class com.runcheck.data.device.DeviceProfile { *; }

# Enum fields in DeviceProfile — Gson calls Enum.valueOf() via reflection
-keepclassmembers enum com.runcheck.domain.model.CurrentUnit { *; }
-keepclassmembers enum com.runcheck.domain.model.SignConvention { *; }

# ── Workers ──────────────────────────────────────────────────────────────────
# WorkManager persists worker class names in its database and instantiates via
# reflection (HiltWorkerFactory / default WorkerFactory). Renaming breaks enqueued work.
-keep class * extends androidx.work.ListenableWorker { *; }

# ── Glance widgets ───────────────────────────────────────────────────────────
# Manifest-declared receivers — Android resolves by class name
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# ── NDT7 speed test ─────────────────────────────────────────────────────────
# The NDT7 AAR (1.0.0) ships consumer rules that reference "NDTTest" (wrong case,
# actual class is NdtTest) — those rules are dead. We provide correct rules here.
-keep class net.measurementlab.ndt7.android.NdtTest { *; }
-keep class net.measurementlab.ndt7.android.NdtTest$* { *; }
# All model classes — deserialized from JSON WebSocket messages via Gson internally
-keep class net.measurementlab.ndt7.android.models.** { *; }
-keep class net.measurementlab.ndt7.android.utils.DataConverter { *; }
