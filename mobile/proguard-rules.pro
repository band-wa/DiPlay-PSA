# R8 rules for the DiPlay release build.
#
# The app performs no reflection of its own. The only Class.forName call is
# LocalOnlyHotspotRadioInfo resolving the platform class
# android.net.wifi.WifiManager$SoftApCallback, and BouncyCastle is used through
# its low-level org.bouncycastle.crypto / org.bouncycastle.asn1 API, so every
# reference is visible to R8 without keep rules.

# Keep every name. The app and its exported reports identify failures by class and
# enum names (for example "stage=${status.javaClass.simpleName}" and
# "failureClass=${error.javaClass.simpleName}"), so obfuscated names make remote
# diagnosis on a head unit impractical. Shrinking and optimisation stay enabled.
-dontobfuscate

# Keep crash reports readable: real source file names and line numbers.
-keepattributes SourceFile,LineNumberTable

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
