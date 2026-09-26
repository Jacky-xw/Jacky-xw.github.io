# 实修日 V1.23 最终 APK：GitHub 一键构建

本工程包含 `.github/workflows/build-v123-final.yml`。上传到 GitHub 后，无需本机安装 Android Studio、Android SDK 或 Gradle。

1. 在 GitHub 新建一个私有仓库。
2. 将本 ZIP 解压后的全部文件上传到仓库根目录（必须包含隐藏目录 `.github`）。
3. 打开仓库的 **Actions** 页面。
4. 选择 **Build 实修日 V1.23 Final APK**。
5. 点击 **Run workflow**。
6. 构建成功后，在该次运行页面底部的 **Artifacts** 下载 `实修日_V1.23_Final_APK`。
7. 解压 Artifact，即得到 `实修日_V1.23_Final.apk`。

说明：V1.23 已定义为最终版本、不再更新，因此工作流使用仅服务于该最终 APK 的独立签名。不要用它继续发布后续版本。
