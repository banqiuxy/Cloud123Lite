# Cloud123

<div align="center">

**第三方 123云盘 Android 客户端**

第三方123云盘客户端、去除官方客户端的广告和下载流量限制。

</div>

## 简介

Cloud123Lite 是一款 **轻量化** 的 **第三方** 123云盘(123pan) Android客户端，提供无广告、简洁流畅、现代化的文件管理体验。去除了官方客户端的下载流量限制，支持批量下载、上传、断点续传，支持账号密码登录、扫码登录、cookie鉴权登录等多种登录方式。

> [!IMPORTANT]
> 本项目为个人学习与技术研究项目，与 123 云盘官方无任何关联。
> 客户端能力依赖 123 云盘服务端接口，部分功能可能随服务端策略调整而变化。
- 交流群：1051025890

## 功能特性

- **账号体系**
  - 账号密码登录
  - 扫码登录（123 云盘 App）
  - Token / Cookie 导入
  - 登录状态本地加密保存（EncryptedSharedPreferences）

- **文件管理**
  - 文件 / 文件夹列表，路径面包屑导航
  - 全局搜索、排序（名称 / 大小 / 时间）、类型筛选
  - 新建文件夹、重命名、移动、复制、删除
  - 回收站：恢复、彻底删除、清空
  - 多选批量操作

- **传输管理**
  - 文件上传 / 下载任务队列，Room 持久化
  - 断点续传、暂停 / 继续 / 取消 / 失败重试
  - 并发任务数控制、后台任务通知

- **分享**
  - 生成分享链接（支持提取码与有效期）
  - 分享记录管理：复制链接、取消分享

- **相册**
  - 图片 / 视频网格浏览
  - 大图预览、批量下载 / 删除

- **界面与体验**
  - Material 3 + Backdrop 液态玻璃控件与容器
  - 浅色 / 深色 / 跟随系统主题，动态色彩
  - 手机 / 平板自适应

## 界面预览

> 截图待补充

## 技术栈

| 层 | 技术 |
|----|------|
| 语言 | Kotlin 2.x |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM（ViewModel + StateFlow），单向数据流 |
| 依赖注入 | Hilt |
| 网络 | Retrofit + OkHttp + Kotlin Serialization |
| 本地存储 | Room、DataStore、EncryptedSharedPreferences |
| 图片加载 | Coil 3 |
| 后台任务 | WorkManager |
| 玻璃效果 | Backdrop 1.0.0（共享玻璃控件、容器和交互 Dock） |

## 构建

环境要求：

- Android Studio（或 AndroidIDE 等支持 Gradle 的 IDE）
- JDK 17+
- Android SDK 36

```bash
# 克隆项目
git clone https://github.com/yourname/Cloud123Lite.git
cd Cloud123Lite

# 构建 Debug APK
./gradlew :app:assembleDebug
```

生成的 APK 位于 `app/build/outputs/apk/debug/`。

### 主题与液态玻璃

- 未保存外观设置时默认使用浅色主题并开启动态色彩；升级不会覆盖已保存的选择。
- 动态色彩在 Android 12 及以上生效，旧系统使用内置配色。
- Dock 和设置页开关使用 Backdrop：Android 13+ 启用透镜折射与高光，Android 12 使用模糊玻璃，Android 10–11 使用清晰的非折射表面。关闭“液态玻璃效果”后关闭这些效果。
- Dock 的采样层只记录背景和页面，不包含 Dock 自身；开关滑块只采样独立轨道，避免循环采样。
- Backdrop 固定为 Android 稳定版 1.0.0，配套 Kotlin 2.2.21、KSP 2.2.21-2.0.4 和 Compose BOM 2025.11.01；不要单独降级编译器或 Compose。

手动验收：首次安装的默认外观；保存设置后重启；浅色/深色/系统主题；动态色彩开关；玻璃开关；四个标签切换及列表滚动；底部多选操作栏与上传按钮不被 Dock 遮挡；TalkBack 能读出开关名称和状态。

## 使用说明

1. 安装 APK 后打开应用
2. 使用 123 云盘账号密码登录，或使用 App 扫码登录
3. 登录后即可浏览网盘文件、上传下载、创建分享

## 目录结构

```
app/src/main/kotlin/com/banqiu/thirdparty123pan/
├── data/          # 网络层、本地数据库、数据仓库、传输引擎
├── di/            # Hilt 依赖注入模块
├── domain/        # 领域模型与仓库接口
├── ui/            # Compose 页面、主题、通用组件
└── util/          # 工具类（设备指纹、MD5、格式化等）
```

## 免责声明

本项目为第三方非官方客户端，仅供个人学习与技术研究使用。请遵守 123 云盘用户协议及相关法律法规，请勿将本项目用于商业用途。使用者应自行承担账号限制、数据丢失等风险。

## 许可证

[GNU General Public License v3.0](LICENSE)


### 交互 Dock 与自定义下载目录

- 点击标签立即切页；长按当前标签进入拖动，玻璃滑块跟手预览，松手才切换。移出 Dock 后松手或退到后台取消本次拖动。支持 RTL 和无障碍标签选择。
- 按钮、输入框、菜单、对话框、底部面板、开关、工具栏和卡片使用共享玻璃样式。文字、图片和文件列表行不额外玻璃化；关闭玻璃后停用背景录制和折射。
- 新下载默认写入 **公共存储 / Download / Cloud123Lite**。设置 → 下载位置通过系统文件夹选择器更换目录，也可以恢复默认。不申请所有文件访问权限。
- 新任务保存创建时的目标目录；改设置不会移动旧文件、修改旧任务或释放旧任务仍需要的目录授权。
- 同名下载等待用户选择覆盖、重命名或取消。相同目标串行检查，不影响其他文件并发下载。系统不允许覆盖的文件会报告授权错误，不绕过系统权限。
- 新下载先保存在应用专属 `download-staging` 目录，校验完整长度后提交到 MediaStore/SAF；提交失败保留暂存文件以便重试。需要同时容纳暂存和目标文件的空间。
- SAF 提供方不一定支持原子覆盖；明确确认覆盖后，提交失败可能使已有目标内容不完整，但完整暂存文件仍保留，可重新授权后重试。不会删除用户已有文件或上传源文件。
- Room 数据库通过显式 2 → 3 迁移增加目标和提交状态，保留旧任务的路径与状态；不再使用破坏性迁移。

### 回归检查

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --max-workers=2 --no-parallel
python scripts/check_transfer_migration.py
```

如果 Windows 中文用户目录使 Gradle 测试进程报 `GradleWorkerMain` 找不到，可使用同一套已编译 JUnit 测试的直接运行方式（不是跳过测试）：

```powershell
.\gradlew.bat :app:exportUnitTestClasspath :app:assembleDebug --max-workers=2 --no-parallel
python scripts/run_junit_windows.py
```

单元测试覆盖文件名安全、Range 校验、真实 HTTP 模拟响应的续传/重启/断流/取消/提交失败，以及 Dock 的 LTR/RTL 坐标映射。SQLite 迁移检查将实际迁移 SQL 与 Room 导出 schema 比较，并校验旧任务未丢失。
设备验收仍需覆盖系统目录授权、MediaStore/SAF 提供方行为、长按手势和 GPU 折射；JVM 测试不等于设备验收。
