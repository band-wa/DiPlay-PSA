# R8 rules for the DiPlay release build.
#
# The app performs no reflection of its own. The only Class.forName call is
# LocalOnlyHotspotRadioInfo resolving the platform class
# android.net.wifi.WifiManager$SoftApCallback, and BouncyCastle is used through
# its low-level org.bouncycastle.crypto / org.bouncycastle.asn1 API, so every
# reference is visible to R8 without keep rules.

# Keep crash reports and the diagnostic logs readable after obfuscation.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Runtime annotations are read back by Compose and by the diagnostics helpers.
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Suppress unrelated optional-dependency warnings from the media3 codecs.
-dontwarn org.checkerframework.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
