# RuruPractice V1.22

面向在家修行者的实修记录与训练辅助 Android 应用。

## 当前版本

- versionName: 1.22.0
- versionCode: 122
- compileSdk: 36
- targetSdk: 36
- minSdk: 26
- Java/Kotlin JVM: 17

## V1.22 重点

- 安般念 16 步、经行、护根、五盖、戒行等训练模块
- 四念处、缘起、五蕴、七觉分、五根五力学习与观察
- 今日修行反馈与近七日统计
- 八戒／简化日当天状态持久化
- 安般念与经行活动计时状态恢复
- Room V2 → V3 → V4 非破坏式迁移
- Android 15/16 边到边布局适配

## 构建

建议使用 Android Studio 打开工程，并安装 Android SDK 36。

Linux/macOS 可运行：

```bash
./gradlew assembleDebug
```

工程内提供的 `gradlew` 是轻量 bootstrap 脚本，会在本机没有对应 Gradle 时尝试下载 Gradle 8.11.1；它不是传统 Gradle Wrapper JAR 的完整副本。

Windows 可使用 Android Studio，或安装 Gradle 8.11.1 后运行 `gradlew.bat`。
