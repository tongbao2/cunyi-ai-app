# 村医AI - 本地离线 AI 助手

基于 [LiteRT-LM (Google)](https://github.com/google-ai-edge/LiteRT-LM) 和 **Gemma 4** 的村医 AI 应用，支持 Android 和 iOS 双平台本地离线运行，模型首次运行时自动从 `hf-mirror.com` 下载。

> 模型文件：`gemma-4-E2B-it-Q4_K_M.gguf`（约 2.5 GB）

---

## 📱 快速预览

| 功能 | 说明 |
|------|------|
| 完全离线 | 下载一次，永久离线使用 |
| 本地推理 | 模型在设备本地运行，无数据上传 |
| 硬件加速 | Android: GPU/NPU；iOS: Apple Neural Engine |
| 村医角色 | 内置 prompt，支持中文乡村医疗问诊 |
| 双平台 | Android (Kotlin) + iOS (SwiftUI) |

---

## 🏗️ Android 端构建

### 环境要求

- **Android Studio** Hedgehog (2023.1.1) 或更高
- **Android SDK** API 34+
- **NDK** r26+（用于编译 LiteRT-LM C++ JNI 层）
- **CMake** 3.28+
- **macOS** / Linux（NDK 交叉编译）

### 构建步骤

```bash
cd android

# 1. 同步 Gradle 依赖
./gradlew --stop
./gradlew assembleDebug --no-daemon

# 或通过 Android Studio 打开 android/ 目录，构建 app 模块

# 2. 生成的 APK 位于：
# app/build/outputs/apk/debug/app-debug.apk
```

### 首次克隆后需要做的事

```bash
# 初始化 LiteRT-LM 子模块
cd ../shared-assets
git submodule add https://github.com/google-ai-edge/LiteRT-LM.git litert-lm
```

### 替换模型下载地址（可选）

编辑 `app/src/main/java/com/cunyi/ai/model/LiteRTEngine.kt`：

```kotlin
// 默认使用 HuggingFace 镜像
val modelUrl: String = "https://hf-mirror.com/unsloth/gemma-4-E2B-it-GGUF/resolve/main/gemma-4-E2B-it-Q4_K_M.gguf"
```

### ProGuard / R8

已内置 `app/proguard-rules.pro`，混淆时保留 Hilt 注解和 JNI 接口。

---

## 🍎 iOS 端构建

### 环境要求

- **Xcode** 15.0+
- **macOS** 13.0+（Xcode 15 最低要求）
- **iOS** 16.0+（支持 iPhone 8 及以上）
- ~10 GB 磁盘空间（模型文件 + llmfarm 编译产物）

### 构建步骤

```bash
cd ios

# 1. 安装 XcodeGen（如果未安装）
brew install xcodegen

# 2. 生成 Xcode 项目
xcodegen generate

# 3. 打开 Xcode
open CunyiAI.xcodeproj

# 4. 在 Xcode 中：
#    - 选择目标设备（模拟器或真机）
#    - 设置签名 Team（Signing & Capabilities）
#    - Product > Build (Cmd+B)
#    - Product > Run (Cmd+R)
```

### 核心依赖说明

| 依赖 | 用途 |
|------|------|
| `llmfarm_core` | llama.cpp Swift 绑定，运行 GGUF 模型 |
| `SnapKit` | Auto Layout DSL |
| 模型文件 | `Documents/models/gemma-4-E2B-it-Q4_K_M.gguf` |

### iOS 模型运行原理

```
ModelManager (Swift)
    ↓ load GGUF
llmfarm_core (llama.cpp C wrapper)
    ↓ GPU/NPU 加速
Apple Neural Engine / GPU
    ↓ token stream
UI 展示
```

---

## ⚙️ 模型下载机制

两个平台均实现相同逻辑：

```
App 启动
  ├─ 检查 modelFile.exists()?
  │    ├─ 是 → 直接加载到推理引擎
  │    └─ 否 → 显示"下载模型"引导页
  │
  ├─ 用户点击"开始下载"
  │    ├─ 发 HEAD 请求获取文件大小
  │    ├─ 下载到 filesDir/models/
  │    └─ 完成后自动加载
  │
  └─ 模型就绪 → 正常聊天
```

---

## 📂 项目结构

```
cunyi-ai-app/
├── android/                    # Android 项目 (Kotlin + Jetpack Compose)
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── cpp/            # LiteRT-LM JNI 绑定 (C++)
│   │   │   ├── java/com/cunyi/ai/
│   │   │   │   ├── model/      # LiteRTEngine, ModelDownloader
│   │   │   │   ├── ui/         # Compose UI
│   │   │   │   ├── di/         # Hilt DI
│   │   │   │   └── MainActivity.kt
│   │   │   └── res/            # 资源文件
│   │   └── build.gradle.kts
│   └── build.gradle.kts
│
├── ios/                        # iOS 项目 (SwiftUI + llmfarm)
│   ├── CunyiAI/
│   │   ├── Sources/
│   │   │   ├── Views/          # SwiftUI 视图
│   │   │   ├── ViewModels/     # MVVM
│   │   │   ├── Models/         # 数据模型
│   │   │   ├── Services/       # ModelManager (llmfarm 封装)
│   │   │   └── AppDelegate.swift
│   │   └── Resources/          # Info.plist, LaunchScreen
│   └── project.yml             # XcodeGen 配置
│
└── shared-assets/
    └── litert-lm/              # LiteRT-LM 源码 (git submodule)
```

---

## 🔧 技术栈

| 层级 | Android | iOS |
|------|---------|-----|
| UI | Jetpack Compose | SwiftUI |
| 架构 | MVVM + Hilt | MVVM + Combine |
| 推理引擎 | LiteRT-LM (C++/JNI) | llmfarm_core (llama.cpp) |
| 硬件加速 | GPU/NPU via LiteRT | Apple Neural Engine |
| 下载 | OkHttp | URLSession |
| 状态持久化 | DataStore | UserDefaults |
| 导航 | Navigation Compose | NavigationStack |
| 最小 SDK | API 26 (Android 8.0) | iOS 16.0 |

---

## 🚀 如何替换模型

只需修改两处：

**Android** — `LiteRTEngine.kt`:
```kotlin
val modelUrl = "你的模型URL"
val modelFileName = "your-model.gguf"
```

**iOS** — `ModelManager.swift`:
```swift
let modelURL = "你的模型URL"
let modelFileName = "your-model.gguf"
```

---

## ⚠️ 已知限制

1. **iOS 模型加载时间**：首次在真机上加载 2.5 GB GGUF 约需 2-5 分钟（取决于设备）
2. **Android NDK 编译**：LiteRT-LM 完整编译需要 ~15 分钟（仅首次）
3. **Gemma 4 量化**：使用 Q4_K_M 量化，精度与速度平衡

---

## 📄 许可证

- 村医AI App 代码：**MIT**
- Gemma 4 模型：[Google Gemma Terms of Use](https://ai.google.com/gemma/terms)
- LiteRT-LM：[Apache 2.0](https://github.com/google-ai-edge/LiteRT-LM/blob/main/LICENSE)
