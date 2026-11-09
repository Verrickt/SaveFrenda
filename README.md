# SaveFrenda

针对小米 HyperOS 系统的轻量级 LSPosed 模块，专注于去除锁屏状态栏双卡运营商之间的竖线分隔符（`|`），采用方案 B 紧凑拼接显示。

## 特性

- **轻量无后台**：采用无界面设计（Headless），无需伴侣 App，零额外常驻内存与电量消耗。
- **天然防冲突**：严格限定目标进程为 `com.android.systemui`，且只拦截锁屏/Keyguard 维度的运营商文本控件，不与 HyperCeiler 或其他控制中心模块产生冲突。
- **全管理器自动作用域**：
  - 兼容经典 LSPosed（Manifest 预声明 `xposedscope`）。
  - 兼容现代 Vector 管理器（内置 `META-INF/xposed/scope.list` 与 `assets/xposed/scope.list`）。
- **CI/CD 全自动化**：支持 GitHub Actions 自动编译、签名与发布 Releases。

## 本地构建

### 前置要求
- JDK 17+
- Android SDK (API 35, Build-Tools 35.0.0+)

### 命令行编译
```bash
./gradlew assembleDebug
```
产物位于：`app/build/outputs/apk/debug/SaveFrenda.apk`。

## GitHub Actions 自动构建与签名

向私有仓库推送代码或打版本 Tag 时会自动触发构建，并在 Releases 中附加签名好的 APK。

### GitHub Secrets 配置项
在仓库 `Settings -> Secrets and variables -> Actions` 中配置以下 4 个密钥：
- `SIGNING_KEY`: Base64 编码的 `.jks` 密钥内容
- `ALIAS`: 密钥别名（如 `savefrenda`）
- `KEY_STORE_PASSWORD`: Keystore 密码
- `KEY_PASSWORD`: 密钥密码

*(若未配置 Secrets，CI 依然能正常编译生成未签名的 Release 包作为 Artifact)*

## 安装与生效
1. 安装打包出的 `SaveFrenda.apk`。
2. 打开 LSPosed / Vector 管理器，在模块列表中开启 **SaveFrenda**（系统界面 / SystemUI 会被自动勾选）。
3. Root 环境下执行重启状态栏生效：
   ```bash
   su -c pkill -f com.android.systemui
   ```
