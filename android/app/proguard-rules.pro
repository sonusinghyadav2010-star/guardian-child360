
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/Cellar/android-sdk/24.3.3/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# react-native-reanimated
-keep class com.swmansion.reanimated.** { *; }
-keep class com.facebook.react.turbomodule.** { *; }

# Add any project specific keep options here:

# WebRTC
-keep class org.webrtc.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-keepnames class com.google.android.gms.tasks.OnFailureListener
-keepnames class com.google.android.gms.tasks.OnSuccessListener

# React Native
-keep class com.facebook.react.** { *; }
-keep class com.facebook.hermes.unicode.** { *; }
-keepclassmembers class com.facebook.react.bridge.JavaScriptModule { *; }

# Other dependencies
-keep class expo.modules.** { *; }
-keep public class com.horcrux.svg.** { *; }
