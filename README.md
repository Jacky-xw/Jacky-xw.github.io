# Edge TTS MP3 for Android

一个尽量轻量、无后端的 Android 文本转 MP3 应用。

## 功能

- 输入或粘贴文本
- 内置常用中文 Edge Neural Voice
- 调整语速、音调、音量
- 长文本自动分块
- 生成 `audio-24khz-48kbitrate-mono-mp3`
- 保存到 `Downloads/EdgeTTS`
- 生成后可直接播放或分享
- 不需要 OpenAI / Azure API Key

## 重要说明

本项目没有把 Python 运行时塞进 APK，而是用原生 Java 实现 Python `edge-tts` 使用的 Microsoft Edge Read Aloud WebSocket 协议。这样 APK 更小，也避免 `aiohttp` 等 Python Android 依赖的兼容问题。

它不是 Microsoft 官方 Azure Speech API。服务端协议可能变化，因此应用未来可能需要更新。应用在首次握手遇到 403 时，会尝试根据服务端时间校准时钟，并从 `edge-tts` 上游仓库刷新当前 Chromium 兼容版本后重试一次。

参考实现：

- https://github.com/rany2/edge-tts
- https://github.com/rany2/edge-tts/blob/master/src/edge_tts/communicate.py
- https://github.com/rany2/edge-tts/blob/master/src/edge_tts/constants.py
- https://github.com/rany2/edge-tts/blob/master/src/edge_tts/drm.py

## 系统要求

- Android 10（API 29）或更高
- 网络连接

## 最省事的 APK 构建方式：GitHub Actions

1. 新建一个 GitHub 仓库。
2. 把本项目所有文件上传到仓库根目录。
3. 打开仓库的 **Actions** 标签。
4. 选择 **Build Android APK**。
5. 点击 **Run workflow**。
6. 构建完成后，在该次工作流页面的 **Artifacts** 下载 `EdgeTTS-MP3-APK`。
7. 解压后得到可安装的 `EdgeTTS-MP3.apk`。

工作流会自动安装 Android SDK 35、Gradle 8.11.1，并构建已由 Android debug key 签名的可安装 APK。

## Android Studio 构建

使用 Android Studio 打开项目，然后执行：

`Build > Build Bundle(s) / APK(s) > Build APK(s)`

默认 debug APK 路径：

`app/build/outputs/apk/debug/app-debug.apk`

## “减少 AI 味”的实用参数

这不是音色克隆或真人录音处理，仅通过 Edge Neural Voice 和韵律参数改善自然度。中文可先尝试：

- 晓晓：语速 `-4%` 到 `-8%`，音调 `0Hz` 附近
- 云希：语速 `-3%` 到 `-7%`
- 文本中保留逗号、句号、问号和自然段落，比把一大段文字连续堆在一起更自然

