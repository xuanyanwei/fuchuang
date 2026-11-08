# 极光流媒体播放器 (Aurora Streaming Player)

基于现代 Android 架构开发的高性能流媒体与 M3U8 播放器，采用 Kotlin、Jetpack Compose 与 Google AndroidX Media3 (ExoPlayer) 构建。

## 核心特性

- **现代 Android 技术栈**: 纯原生 Kotlin + Jetpack Compose Material 3 界面与 AndroidX Media3 (ExoPlayer) 硬解码引擎。
- **M3U / M3U8 智能解析与分集**: 智能识别 `#EXTM3U` 格式，自动检测剧集编号（如第01集、EP01）并按频道和系列分集编组，支持流媒体选集与连续自动播放。
- **自定义 HTTP 请求头**: 支持全局与独立注入 User-Agent（提供 Chrome、Safari、Android TV 预设）、Referer 防盗链、Origin、Cookie 与 Token 鉴权头。
- **全屏沉浸式播放体验**: 支持传感器重力横屏全屏、手势滑动、画面比例自适应（适应/填充/16:9/4:3）、多档倍速播放（0.5x - 2.0x）与屏幕误触锁。
- **画中画模式 (PiP)**: 原生支持 Android 8.0+ Picture-in-Picture 系统级画中画小窗浮动播放。
- **播放历史与无缝续播**: 自动记录观看位置与进度，支持一键断点续播。
- **Google Play 与创作者合规**: 无商业广告 SDK，遵循最小权限原则（仅请求 INTERNET 与网络状态）。

## 构建要求

- **JDK**: 21
- **Gradle**: 9.3.1
- **Android SDK**: 36 (Min SDK 24, Target SDK 34)
- **AGP**: 9.1.1
