#!/usr/bin/env bash
set -euo pipefail
project="$(cd "$(dirname "$0")" && pwd)"
toolchain="${ANDROID_LOCAL_TOOLS:?Set ANDROID_LOCAL_TOOLS to Android build-tools/ecj toolchain root}"
build="$project/build"; out="$project/../out"
python3 - "$build" <<'CLEAN'
from pathlib import Path
import shutil,sys
for name in ('classes','dex'):
 shutil.rmtree(Path(sys.argv[1])/name,ignore_errors=True)
CLEAN
mkdir -p "$build/classes" "$build/dex" "$out"
java -jar "$toolchain/ecj.jar" -8 -warn:none -bootclasspath "$toolchain/android-35/android.jar:$toolchain/android-15/core-lambda-stubs.jar" -d "$build/classes" "$project"/app/src/main/java/cn/pi/overtime/*.java
"$toolchain/android-15/aapt" package -f -M "$project/app/src/main/AndroidManifest.xml" -S "$project/app/src/main/res" -I "$toolchain/android-35/android.jar" -F "$build/unsigned.apk"
python3 - "$build" <<'PY'
import zipfile,pathlib,sys
p=pathlib.Path(sys.argv[1])
with zipfile.ZipFile(p/'classes.jar','w') as z:
 for f in (p/'classes').rglob('*.class'):z.write(f,str(f.relative_to(p/'classes')))
PY
"$toolchain/android-15/d8" --min-api 26 --lib "$toolchain/android-35/android.jar" --output "$build/dex" "$build/classes.jar"
(cd "$build/dex" && "$toolchain/android-15/aapt" add "$build/unsigned.apk" classes*.dex)
"$toolchain/android-15/zipalign" -f -p 4 "$build/unsigned.apk" "$build/aligned.apk"
: "${KEYSTORE_PATH:?Set KEYSTORE_PATH to the existing release keystore; never generate a replacement key}"
: "${KEYSTORE_PASSWORD:?Set KEYSTORE_PASSWORD}"
: "${KEY_ALIAS:?Set KEY_ALIAS}"
: "${KEY_PASSWORD:?Set KEY_PASSWORD}"
if [ ! -f "$KEYSTORE_PATH" ]; then
 echo "Signing keystore not found: $KEYSTORE_PATH" >&2
 exit 1
fi
"$toolchain/android-15/apksigner" sign --ks "$KEYSTORE_PATH" --ks-pass env:KEYSTORE_PASSWORD --ks-key-alias "$KEY_ALIAS" --key-pass env:KEY_PASSWORD --out "$out/Overtime-1.5.0.apk" "$build/aligned.apk"
"$toolchain/android-15/apksigner" verify --verbose "$out/Overtime-1.5.0.apk"
"$toolchain/android-15/zipalign" -c 4 "$out/Overtime-1.5.0.apk"
