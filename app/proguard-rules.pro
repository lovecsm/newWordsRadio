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
-keep class com.iflytek.** { *; }         #保持com.iflytek.**这个包里面的所有类和所有方法不被混淆。

-dontwarn com.iflytek.**          #让ProGuard不要警告找不到com.iflytek.**这个包里面的类的相关引用

-keep class com.word.radio.ifly.** { *; }
-dontwarn com.word.radio.ifly.**

-keep class com.android.support.** {*; }
-dontwarn com.android.support.**

-keep public class cn.waps.** {*;}
-keep public interface cn.waps.** {*;}
-dontwarn cn.waps.**