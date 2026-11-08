#!/usr/bin/env bash
set -e

echo "=== Preparing Build Tools ==="
mkdir -p /usr/local/bin /usr/local/lib /usr/lib/android-sdk/platforms/android-34

# 1. Ensure android.jar
if [ ! -f /usr/lib/android-sdk/platforms/android-34/android.jar ]; then
  echo "Downloading Android platform 35 jar..."
  curl -fsSL -o /tmp/platform-35.zip https://dl.google.com/android/repository/platform-35_r02.zip
  unzip -q -j /tmp/platform-35.zip "android-35/android.jar" -d /usr/lib/android-sdk/platforms/android-34/
  rm -f /tmp/platform-35.zip
fi

# 2. Ensure aapt2
if [ ! -f /usr/local/bin/aapt2 ]; then
  echo "Downloading aapt2..."
  curl -fsSL -o /tmp/aapt2.jar https://dl.google.com/android/maven2/com/android/tools/build/aapt2/8.2.2-10154469/aapt2-8.2.2-10154469-linux.jar
  unzip -q -o /tmp/aapt2.jar aapt2 -d /usr/local/bin
  chmod +x /usr/local/bin/aapt2
  rm -f /tmp/aapt2.jar
fi

# 3. Ensure Google R8 D8
if [ ! -f /usr/local/lib/r8.jar ]; then
  echo "Downloading r8.jar..."
  curl -fsSL -o /usr/local/lib/r8.jar https://dl.google.com/android/maven2/com/android/tools/r8/8.2.42/r8-8.2.42.jar
fi

echo "=== Building Aurora Player APK ==="

# 4. Sync web bundle to android assets
mkdir -p /app/applet/android/app/src/main/assets/dist
cp -r /app/applet/dist/* /app/applet/android/app/src/main/assets/dist/
rm -f /app/applet/android/app/src/main/assets/dist/*.apk

# 5. Compile Java classes
rm -rf /tmp/apk_build
mkdir -p /tmp/apk_build/classes /tmp/apk_build/res_compiled
javac -cp /usr/lib/android-sdk/platforms/android-34/android.jar -d /tmp/apk_build/classes \
  /app/applet/android/app/src/main/java/com/aurora/player/MainActivity.java

# 6. Dex with Google D8
java -cp /usr/local/lib/r8.jar com.android.tools.r8.D8 \
  --release \
  --min-api 24 \
  --lib /usr/lib/android-sdk/platforms/android-34/android.jar \
  --output /tmp/apk_build \
  /tmp/apk_build/classes/com/aurora/player/*.class

# 7. Compile resources and link with aapt2
/usr/local/bin/aapt2 compile --dir /app/applet/android/app/src/main/res -o /tmp/apk_build/res_compiled.zip
/usr/local/bin/aapt2 link -o /tmp/apk_build/app-unsigned.apk \
  -I /usr/lib/android-sdk/platforms/android-34/android.jar \
  --manifest /app/applet/android/app/src/main/AndroidManifest.xml \
  -A /app/applet/android/app/src/main/assets \
  --min-sdk-version 24 \
  --target-sdk-version 34 \
  --version-code 1 \
  --version-name "1.0.0" \
  --auto-add-overlay \
  /tmp/apk_build/res_compiled.zip

# 8. Add classes.dex
cd /tmp/apk_build
jar uf app-unsigned.apk classes.dex

# 9. Zipalign
zipalign -f -v 4 app-unsigned.apk app-aligned.apk

# 10. Apksigner with V1 + V2 + V3
mkdir -p /tmp/keystore
if [ ! -f /tmp/keystore/release.keystore ]; then
  keytool -genkeypair -v \
    -keystore /tmp/keystore/release.keystore \
    -alias aurora \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass 123456 \
    -keypass 123456 \
    -dname "CN=Aurora, OU=Player, O=Aurora, L=Beijing, ST=Beijing, C=CN"
fi

apksigner sign \
  --ks /tmp/keystore/release.keystore \
  --ks-pass pass:123456 \
  --ks-key-alias aurora \
  --key-pass pass:123456 \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out /tmp/apk_build/aurora-player.apk \
  app-aligned.apk

# 11. Copy to dist and public
cp /tmp/apk_build/aurora-player.apk /app/applet/dist/aurora-player.apk
cp /tmp/apk_build/aurora-player.apk /app/applet/public/aurora-player.apk

echo "=== APK Built successfully: /dist/aurora-player.apk ==="
/usr/local/bin/aapt2 dump badging /app/applet/dist/aurora-player.apk | head -n 8
