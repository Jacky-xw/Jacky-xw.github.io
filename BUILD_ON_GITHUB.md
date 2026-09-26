# 实修日 V1.25 APK：GitHub 构建

上传到 GitHub 后，可使用 Android 构建环境生成 V1.25 APK；工程本身仍保持完全离线运行，不包含登录、云端或网络权限。

1. 在 GitHub 新建一个私有仓库。
2. 将本 ZIP 解压后的全部文件上传到仓库根目录（必须包含隐藏目录 `.github`）。
3. 打开仓库的 **Actions** 页面。
4. 选择仓库中配置的 Android 构建 workflow。
5. 点击 **Run workflow**。
6. 构建成功后，在该次运行页面底部的 Artifacts 下载 APK。

版本号由 `app/build.gradle.kts` 中的 `versionName = "1.25.0"` 和 `versionCode = 126` 管理。
