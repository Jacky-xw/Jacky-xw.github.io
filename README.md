# RuruPractice V1.28.1

面向在家修行者的实修记录与训练辅助 Android 应用。数据只保存在设备本地，不需要登录、云端服务或网络权限。

## 当前版本

- versionName: 1.28.1
- versionCode: 130
- applicationId: `com.ruru.practice`
- compileSdk / targetSdk: 36
- minSdk: 26
- Java / Kotlin JVM: 17

## 主要能力

- 安般念十六步、经行、护根、五盖、戒行与八戒自检
- 四念处、缘起、五蕴等观察记录
- 学习中心：章节索引、阅读位置恢复、阅读设置、分页阅读、月历热力图与趋势图
- 首页今日反馈、近七日统计与混合记录删除
- Room 数据库 V2 → V3 → V4 → V5 非破坏式迁移
- 安般念与经行计时状态恢复，本地引磬试听与自定义音频

## 本地构建

工程使用标准 Gradle Wrapper，Linux/macOS 在项目根目录运行：

```bash
./gradlew testDebugUnitTest assembleDebug
```

需要 JDK 17 和 Android SDK 36。生成的调试 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

Windows 使用：

```bat
gradlew.bat testDebugUnitTest assembleDebug
```

## GitHub Actions

`.github/workflows/android.yml` 会在 push、Pull Request 或手动运行时：

1. 配置 JDK 17 与 Android SDK 36；
2. 执行单元测试和 `assembleDebug`；
3. 将 `app-debug.apk` 上传为构建产物。

上传到 GitHub 时请把 ZIP 解压后的全部内容（包括隐藏目录 `.github`）放在仓库根目录。详见 [BUILD_ON_GITHUB.md](BUILD_ON_GITHUB.md)。

## 升级安装

新 APK 保持原有 `applicationId`，并将 `versionCode` 提升到 130；只有使用与旧 APK 相同的签名密钥时，才能在不卸载旧版本的情况下覆盖更新。当前 workflow 生成的是未签名的 debug APK，适合测试安装；正式升级需要使用原发布签名构建 release APK。
