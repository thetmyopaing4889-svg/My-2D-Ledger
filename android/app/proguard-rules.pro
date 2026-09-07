# Proguard / R8 rules for AegisVPN

# Keep native methods and JNI bindings
-keepclasseswithmembernames class * {
    native <methods>;
}

# Preserve JNI Tunnel Bridge package
-keep class org.anticensor.vpn.core.tunnel.** { *; }
-keepclassmembers class org.anticensor.vpn.core.tunnel.** { *; }

# Keep data models used for JSON config generation and serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp rules
-dontwarn okhttp3.**
-dontwarn okio.**
