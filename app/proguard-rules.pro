# ELVO keep rules.
# Components that Android instantiates by name must keep their original names.
# AGP already derives most of these from the manifest; listing them explicitly
# also protects class-name strings built at runtime (e.g. Accessibility settings).
-keep class com.elvo.app.ElvoApp { *; }
-keep class com.elvo.app.MainActivity { *; }
-keep class com.elvo.app.service.** { *; }
-keep class com.elvo.app.scheduler.** { *; }
