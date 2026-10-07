# 🎵 MusicPauser · 音乐暂停器

一个轻量、开箱即用的 Android 应用，为比亚迪 DiLink 3.0 打造，解决「方向盘音量按键按下是静音而不是暂停」的痛点：提供**悬浮球、桌面组件、应用内按钮**三种方式，一键完成**播放 / 暂停、上一首、下一首**。

[![Version](https://img.shields.io/badge/version-v1.4-blue)](https://github.com/)
[![minSdk](https://img.shields.io/badge/minSdk-26%20(Android%208.0)-green)](https://github.com/)
[![targetSdk](https://img.shields.io/badge/targetSdk-33-orange)](https://github.com/)
[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-brightgreen)](https://github.com/)

---

## ✨ 产品亮点

| 亮点 | 说明 |
| --- | --- |
| 🔓 **无需 Root** | 全程不依赖 root 权限，不动系统分区、不改内核、不注入任何系统进程，安全、零风险。 |
| 🚫 **无需 ADB 授权** | 安装即用，核心的播放控制功能开箱即可工作，不需要连接电脑、不需要敲 `adb` 命令。 |
| ⚡ **安装即可使用** | 车机上直接安装 APK、打开应用即可控制音乐，无需繁琐的调试、授权、配对流程。 |
| 🚗 **为 DiLink 而生，但不限于 DiLink** | 专为**比亚迪 DiLink 3.0** 开发，同时向下兼容、向上适配——**更高版本 DiLink、其他基于 Android 的车机系统，甚至普通 Android 手机**都能用。 |
| **开机自动播放音乐** | 打开开关即可 |
| **悬浮球位置以及相关设置均已持久化** | 熄火后下次上电仍记住上次设置/悬浮球位置 |

**注意注意注意**⚠️：自动播放音乐、自动显示悬浮球需要在“自启动管理”app（DiLink系统） 中将我们的「音乐暂停器」开关关闭，允许我们的应用自启。

---

## 📦 功能特性

- **播放控制**：播放 / 暂停、上一首、下一首
- **音乐源识别**：识别并显示当前音乐软件（Spotify、QQ音乐、网易云音乐、酷我音乐、酷狗音乐、YouTube Music、Apple Music 等），未知应用显示包名
- **歌曲信息显示**：在支持「通知使用权」的设备上显示歌名与歌手
- **桌面组件 (Widget)**：在支持 Widget 的桌面上添加播放控制组件
- **悬浮球**：可拖动、单击播放 / 暂停、长按打开应用、自动记忆位置
- **开机自启**：记住悬浮球开关状态，开机自动恢复
- **开机自动播放**：可设置开机后自动续播上次的音乐（v1.3，仅车机）
- **双模式自适应**：自动适配「通知模式」（有通知权限）与「按键控制模式」（Android 10 及以下 / 无通知权限）

---

## 🖼️ 使用截图

![Music-Pauser使用截图3_compressed.jpg](https://image.940304.xyz/i/2026/09/10/6aa23d88ad129.jpg)
![Music-Pauser使用截图1_compressed.jpg](https://image.940304.xyz/i/2026/09/10/6aa23d88acd45.jpg)
![Music-Pauser使用截图2_compressed.jpg](https://image.940304.xyz/i/2026/09/10/6aa23d88ad0fe.jpg)

## ⚙️ 工作原理

### 通知模式（Android 11+，如手机）

通过「通知使用权」读取音乐 App 的媒体通知，获取歌曲信息与播放状态，并触发其通知栏的播放 / 暂停 / 上一首 / 下一首动作来实现控制。

### 按键控制模式（Android 10 及以下，如比亚迪车机）

车机系统屏蔽了「通知使用权」设置，无法读取通知。此时使用系统级接口 `AudioManager.dispatchMediaKeyEvent()` 发送「媒体按键」事件（等同于按实体播放/暂停键），由系统路由给当前活跃的媒体会话（例如最后一个播放的 Spotify），从而实现播放 / 暂停 / 切歌。此模式下无法显示歌曲信息。

> Android 11 起该接口被移除，因此按键控制模式仅适用于 Android 10 及以下。

---

## 🧩 兼容性

| 设备 | 系统 | 模式 |
| --- | --- | --- |
| 比亚迪海豚 2021 款 DiLink 3.0 | Android 10 (API 29) | 按键控制模式 |
| 更高版本 DiLink / 其他 Android 车机 | Android 10 及以下 → 按键控制；Android 11+ → 通知模式 | 自动适配 |
| 红米 K70（手机） | HyperOS (Android 14/15) | 通知模式 |

- `minSdk` 26（Android 8.0）
- `targetSdk` 33
- 语言：Kotlin

---

## 🚀 安装与使用

1. 下载并安装 APK（见 Releases 中的 `MusicPauser-v1.4.apk`）
2. 打开应用：
   - **手机**：点击「授予通知使用权」，开启本应用的通知权限
   - **车机**：无需通知权限，自动进入「按键控制模式」
3. 音乐开始播放后，应用内按钮、桌面组件、悬浮球即可控制

### 悬浮球

- 应用内点击「启用悬浮按键」，按提示授权「悬浮窗」权限
- 单击悬浮球 = 播放 / 暂停，长按 = 打开应用，拖动 = 移动位置
- 位置自动记忆：拖动后自动保存，重新开启或重启后恢复到上次位置
- 开启后熄火重开机，悬浮球会自动恢复（需允许应用「开机自启」）

> 个别车机若屏蔽了系统「悬浮窗」授权设置页，可选用 ADB 兜底授权：
> `adb shell appops set com.musicpauser SYSTEM_ALERT_WINDOW allow`

### 开机自动播放

- 主界面打开「开机自动播放音乐」开关
- 需同时满足：已开启「悬浮按键」且允许「开机自启」
- 原理：记录「本次用车期间是否出现过播放」（粘性，暂停不会清除），开机并显示悬浮球后延迟 2 秒发送「播放」键，由系统路由给上次的媒体 App（如 Spotify）续播
- 仅对车机（按键控制模式）有效，手机 Android 11+ 无此接口

---

## 🔐 权限说明

| 权限 | 用途 |
| --- | --- |
| `SYSTEM_ALERT_WINDOW` | 悬浮球 |
| `RECEIVE_BOOT_COMPLETED` | 开机自启 |
| `FOREGROUND_SERVICE` | 悬浮球前台服务 |
| `MODIFY_AUDIO_SETTINGS` | 部分系统音频操作（防御性声明） |

---

## 🛠 构建

环境要求：

- JDK 17
- Android SDK（`platforms;android-33`、`build-tools;33.0.2`）
- Gradle 7.6.4（项目已包含 wrapper）

```bash
# 配置 SDK 路径（按需修改）
echo "sdk.dir=/path/to/android-sdk" > local.properties

# 编译 release APK
./gradlew assembleRelease
```

产物位于 `app/build/outputs/apk/release/app-release.apk`。

### 签名

release 构建通过 `keystore.properties` 读取签名配置（参考 `keystore.properties.example` 填写真实值）。`keystore/` 与 `keystore.properties` 已被 `.gitignore` 忽略，请勿提交到公开仓库。

> ⚠️ 该 keystore 是应用升级用的唯一签名凭证，请妥善保管，并使用安全的密码管理方式。

---

## 🗂 项目结构

```
app/src/main/kotlin/com/musicpauser/
├── MainActivity.kt               # 主界面
├── PlaybackManager.kt            # 核心状态机 / 控制逻辑
├── MusicNotificationListener.kt  # 通知监听（通知模式）
├── PlayPauseWidgetProvider.kt    # 桌面组件
├── OverlayService.kt             # 悬浮球
├── BootReceiver.kt               # 开机自启
└── Prefs.kt                      # 状态持久化
```

---

## 📄 版权

© 2026 developed by [hellodk.cn](https://hellodk.cn) & DeepSeek
