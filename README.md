# xAI TTS Lab (Android)

Android client for the xAI Text-to-Speech API.

## Main capabilities

- Secure local API-key storage using Android Keystore (AES/GCM).
- Dynamically loads built-in voices from `GET /v1/tts/voices`; also attempts to load custom voices and supports manual custom `voice_id` entry.
- Languages: `auto` + all currently documented xAI TTS language codes.
- Speed 0.7–1.5, codec, sample rate, MP3 bit rate, streaming-latency optimization, text normalization.
- Character-level timestamps (`with_timestamps`) and SRT export.
- Pronunciation replacement map, including IPA values.
- Buttons for every currently documented inline/wrapping speech tag.
- Advanced JSON override box for experimenting with new/undocumented TTS request fields without rebuilding the app.
- Audio preview for MP3/WAV and Save As through Android's Storage Access Framework.
- SRT defaults to long, one-line subtitle cues and natural punctuation boundaries to avoid strange internal wrapping.

## Build

Open this folder in Android Studio and choose **Build > Build APK(s)**.

Or from a machine with Android SDK + Gradle installed:

```bash
gradle :app:assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Security note

The API key is not hard-coded. When you tap “保存 Key”, it is encrypted with an Android Keystore AES key and only the ciphertext/IV is stored in SharedPreferences.

## API behavior

The app calls only `https://api.x.ai`. API usage and billing are charged by xAI to the API key entered by the user.

## Build without a local Android toolchain (GitHub Actions)

A workflow is included at `.github/workflows/build-apk.yml`. Push this project to a GitHub repository, open **Actions > Build Android APK > Run workflow**, then download the generated `xai-tts-lab-debug-apk` artifact.
