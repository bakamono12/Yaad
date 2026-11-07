#!/usr/bin/env bash
# Downloads the offline Hindi voice (Piper "priyamvada") and the sherpa-onnx
# native libraries into the app. They're ~80 MB, so they aren't committed —
# CI runs this before every build. Run it once locally before building in
# Android Studio.
set -euo pipefail
cd "$(dirname "$0")/.."

SHERPA_VERSION=1.12.40
VOICE=vits-piper-hi_IN-priyamvada-medium
MODEL=hi_IN-priyamvada-medium.onnx
ASSETS=app/src/main/assets
JNI=app/src/main/jniLibs
BASE=https://github.com/k2-fsa/sherpa-onnx/releases/download

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

if [ ! -f "$ASSETS/$VOICE/$MODEL" ]; then
  echo "Downloading Hindi voice…"
  curl -fsSL -o "$tmp/voice.tar.bz2" "$BASE/tts-models/$VOICE.tar.bz2"
  mkdir -p "$ASSETS"
  rm -rf "${ASSETS:?}/$VOICE"
  tar xjf "$tmp/voice.tar.bz2" -C "$ASSETS"
  # Keep only the Hindi and English pronunciation dictionaries (English is for
  # words like "BP"); the other languages are ~17 MB we don't need.
  find "$ASSETS/$VOICE/espeak-ng-data" -maxdepth 1 -name '*_dict' \
    ! -name hi_dict ! -name en_dict -delete
fi

if [ ! -f "$JNI/arm64-v8a/libsherpa-onnx-jni.so" ]; then
  echo "Downloading sherpa-onnx $SHERPA_VERSION native libraries…"
  curl -fsSL -o "$tmp/android.tar.bz2" \
    "$BASE/v$SHERPA_VERSION/sherpa-onnx-v$SHERPA_VERSION-android.tar.bz2"
  tar xjf "$tmp/android.tar.bz2" -C "$tmp"
  for abi in arm64-v8a armeabi-v7a; do
    mkdir -p "$JNI/$abi"
    cp "$tmp/jniLibs/$abi/libsherpa-onnx-jni.so" "$tmp/jniLibs/$abi/libonnxruntime.so" "$JNI/$abi/"
  done
fi

echo "Voice ready:"
du -sh "$ASSETS/$VOICE" "$JNI"
