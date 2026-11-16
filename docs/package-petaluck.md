# Petaluck 包名与源码迁移

2026-11-16，开发版 1.0.21 / versionCode 22。按用户要求，将临时包 `com.example.flowertears.p18ab52cf` 改为 `com.unclekarma.petaluck`。此要求覆盖此前保留开发包名的约束。

- applicationId、namespace、Kotlin 主代码/单测/设备测试的包声明、导入和目录全部迁移。
- Gradle 工程名、主题、容器标识、相关日志标签统一 Petaluck；ADB action 由新 packageName 派生。
- Editor 工程目录和项目文件改为 `art/Petaluck/Petaluck.spatialproject`；参考文档、原始图片/压缩包及图标备选文件改用 petaluck 命名，相关工具引用同步更新。
- 无障碍契约按迁移后的源码重新计算锚点，保留已有语义和手工完成的 petal0 绑定。
- 中文显示名「一瓣欢喜」、英文 Petaluck 和现有玩法不变。历史日志、版本记录、二进制来源信息和旧备份保留原始身份；文档中的已迁移源码链接同步维护。移动的 26 个图片/归档/Editor 二进制文件经 Git blob 哈希比对，与原文件完全相同。

## 验证

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest` 成功；24 个测试套件共139项，0失败/错误/跳过。
- 实验 API 检查通过；SpatialUI 0错误、1条既有的大字号提示；A11Y源码审计0错误/警告。
- AAPT2 核对 APK package、versionName/versionCode；新包已安装到 PB311XHGKC150135G，启动入口 `.platform.LaunchActivity` 正常返回，app info 确认运行及版本。
- 新 action 的实际广播 `color=8`、`color=-1` 均返回 result=0 和对应 selectedColor；结束时为随机 -1。
- 运行时 A11Y 审核返回 TARGET_ROOT_TIMEOUT，未发现可见应用根节点；未声称本次迁移已经完成头显场景显示、交互和花瓣雨回归。

新包与旧包是两个独立应用，旧包未卸载，旧应用数据未自动迁移。既有 release 构建版本名覆写 `V1.0.0` 和 VibeLab 应用配置保持；本次验证的是 debug APK，没有上传/发布。

APK：`backups/package-petaluck-2026-11-16/Petaluck-1.0.21.apk`
SHA-256：`6c9eb8d139b6f4270ffd0fecda1620db65bb7f28e3428230de0d1121d06c0b38`

```sh
adb shell am broadcast -a com.unclekarma.petaluck.DEBUG_ROSE_COLOR -p com.unclekarma.petaluck --ei color 8
```
