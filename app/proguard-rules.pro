# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include localPath and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

#-keepattributes *Annotation*
-keepattributes Signature

-keep public class * extends android.app.Fragment  
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.support.v4.**
-keep public class com.android.vending.licensing.ILicensingService

-keep class com.android.vending.licensing.ILicensingService
-keep class android.support.v4.** { *; }  

-keep class com.umeng.** { *; }  
-keep class com.umeng.analytics.** { *; }  
-keep class com.umeng.common.** { *; }  
-keep class com.umeng.newxp.** { *; }
-keep class com.alibaba.**{ *; }
-keep class com.egame.tv.**{ *; }
-keep class com.baofengtv.middleware.**{ *; }

-keep public interface com.umeng.**
-keep public interface com.alibaba.**
-keep public interface com.egame.tv.**
-keep public interface com.baofengtv.middleware.**

-keep public class com.baofengtv.supporter.R$*{
    public static final int *;
}
-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context);
}

-keepclasseswithmembers class * {
    native <methods>;
}

-dontshrink
-dontoptimize
-dontwarn android.webkit.WebView
-dontwarn com.umeng.**
-dontwarn com.alibaba.**
-dontwarn com.egame.tv.**
-dontwarn com.baofengtv.middleware.**
-dontwarn android.support.v4.**

-keepattributes Exceptions,InnerClasses,Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable



