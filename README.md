# Cloud123

<div align="center">

**第三方 123云盘 Android 客户端**

第三方123云盘客户端、去除官方客户端的广告和下载流量限制

</div>

## 简介

Cloud123Lite 是一款 **轻量化** 的 **第三方** 123云盘(123pan) Android客户端，提供无广告、简洁流畅、现代化的文件管理体验。去除了官方客户端的下载流量限制，支持批量下载、上传、断点续传，支持账号密码登录、扫码登录、cookie鉴权登录等多种登录方式。

> 本项目为个人学习与技术研究项目，与 123 云盘官方无任何关联。

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
  - Material 3 + 液态玻璃（Haze）效果
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
| 玻璃效果 | Haze |

## 构建

环境要求：

- Android Studio（或 AndroidIDE 等支持 Gradle 的 IDE）
- JDK 17+
- Android SDK 36

```bash
# 克隆项目
git clone https://github.com/yourname/Cloud123.git
cd Cloud123

# 构建 Debug APK
./gradlew :app:assembleDebug
```

生成的 APK 位于 `app/build/outputs/apk/debug/`。

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

## 已知问题

当前版本存在以下已知问题，正在排查修复中：

- **无法上传文件**：上传任务在获取 S3 分片预签名地址阶段失败，可能与服务端鉴权策略有关。
- **无法重命名**：重命名接口请求被服务端拒绝。

> 其余功能（登录、文件浏览、下载、删除、回收站、分享记录等）可正常使用。

## 免责声明

本项目为第三方非官方客户端，仅供个人学习与技术研究使用。请遵守 123 云盘用户协议及相关法律法规，请勿将本项目用于商业用途。使用者应自行承担账号限制、数据丢失等风险。

## 许可证

[GNU General Public License v3.0](LICENSE)
