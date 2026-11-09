# SaveFrenda (Save-Frenda)

针对小米 HyperOS 系统的轻量级 LSPosed 模块，专注于去除锁屏状态栏双卡运营商之间的竖线分隔符（`|`）。

## 特性

- **轻量无后台**：采用无界面设计（Headless），无需伴侣 App，零额外常驻内存与电量消耗。
- **天然防冲突**：严格限定目标进程为 `com.android.systemui`，且只拦截锁屏/Keyguard 维度的运营商文本控件，不与 HyperCeiler 或其他控制中心模块产生冲突。
- **自动作用域**：预声明 LSPosed 作用域为 `com.android.systemui`，在 LSPosed 管理器中激活时自动勾选目标。

## 构建方式

### 前置要求
- JDK 17+
- Android SDK (API 35, Build-Tools 35.0.0+)

### 命令行编译
```bash
./gradlew assembleDebug
```
生成的 APK 位于：`app/build/outputs/apk/debug/app-debug.apk`。

## 安装与生效
1. 安装生成的 APK。
2. 打开 LSPosed 管理器，在模块列表中找到并勾选激活 **SaveFrenda**（推荐作用域已默认勾选 `系统界面 / SystemUI`）。
3. 终端执行 `pkill -f com.android.systemui` 或重启手机即可生效。
