# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Puente JavaScript del árbol 3D (HU-38).
# Hoy `isMinifyEnabled = false` y esta regla no hace nada. El día que alguien active la
# minificación, sin ella R8 renombraría los métodos anotados y el síntoma sería un árbol que se
# queda en el fallback nativo **solo en release** — un diagnóstico caro por cuatro líneas.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
