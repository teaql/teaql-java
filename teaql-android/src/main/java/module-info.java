// teaql-android is compiled against the Android SDK stub (provided scope).
// The Android SDK does not ship as a named JPMS module, so it is declared
// as a static (compile-time-only) dependency via the automatic module name
// derived from the android.jar artifact.
//
// Downstream Android Gradle builds do NOT use the Java module system;
// this module-info.java exists solely to bring teaql-android into the
// multi-module JPMS graph for standard Maven/Java-17+ tooling.
module io.teaql.android {
    requires io.teaql.core;
    requires io.teaql.sql.portable;
    requires io.teaql.dataservice.sql;

    // Android SDK classes (SQLiteDatabase etc.) are on the classpath at
    // compile time via the provided-scope android stub artifact.
    // They are not available as a named module, so we use requires static.
    requires static android;

    exports io.teaql.android;
}
