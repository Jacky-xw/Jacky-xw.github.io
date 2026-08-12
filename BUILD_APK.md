# 生成 APK（无需本地安装 Android Studio）

项目已经附带 `.github/workflows/build-apk.yml`。

在 GitHub 网页上：

1. 创建空仓库并上传整个项目。
2. 进入 **Actions**。
3. 点击左侧 **Build Android APK**。
4. 点击 **Run workflow**。
5. 运行完成后，页面底部下载 **EdgeTTS-MP3-APK**。
6. 解压，得到 `EdgeTTS-MP3.apk`。

这是 debug 签名 APK，可直接侧载安装。若手机提示“允许安装未知来源应用”，需要按 Android 系统提示授权你用于打开 APK 的文件管理器/浏览器。
