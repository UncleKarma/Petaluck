# 第二朵花开始眼手交互失效修复

2026-11-17，开发版 **1.0.24 / code25**，包名 `com.unclekarma.petaluck`。

## 故障证据

用户报告第二朵花开始 GazePinch 无法交互，DirectPinch 也异常。设备 PB311XHGKC150135G、进程7812的修复前日志显示：

- 13:24:07–13:24:54：第一轮28片花瓣均有 GazePinch 抓取与摘下记录。
- 13:24:57.231：重玩后的空白 erase 提交完成。
- 13:24:58.354：第二轮30片花瓣完成入场并启用交互；其后截至本次读取没有 PetalGrab。
- 源码 `CompletionBufferGate.committed` 对 erase 同样写入 enabled=true，`syncPanels` 将这一状态写给固定在花前方的560×384dp完成面板。透明和空 Compose 内容没有单独关闭附件的原生命中区域。

这支持“完成面板常驻遮挡下一朵花”的首要根因推断。没有采集到系统命中链，也没有第二轮 DirectPinch 事件，不能据此断言全部近手异常均由此引起。

## 修复

1. 重玩立即遮罩完成面板、移除 Compose 内容，保留 `BlendMode.Clear` 和绑定实际绘制的硬件 frame-commit 回调。
2. 仅在匹配的 erase 回调到达后停用 native owner；Loading ID 变化不丢失 erase，旧 reveal/erase 回调继续由 serial 拒绝。
3. 下一轮装配通过现有 `SceneFrameGate.awaitReady` 等待逻辑 gate 与实际 owner 都为 disabled；最大600个实际场景帧，超限走既有 Error/重试路径。没有新增固定延时或用帧数代替绘制提交。
4. 下一次结束依然先遮罩启用、提交新的空白帧，再显示祝福/按钮。固定位置、尺寸、600/450ms渐显和手势计算不变。
5. `RoundInputReady` 与 `CompletionFrameCommitted` 增加 owner enabled 回读及 `replay-hit-surface-v1` 标记，便于复测判定。

此修复取代 `finale-refinements-v2` 的“完成面板在轮次之间保持启用”策略。Android提交仍不是空间合成器的呈现栅栏，结束闪帧必须复测。

## 验证与交付

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest`：成功；139测试，0失败/错误/跳过。完成面板测试覆盖三轮 erase 后停用、每轮新的 reveal、Loading ID切换、旧回调与owner替换。
- 安装版本的实验 API checker：PASS；SpatialUI verifier：0 errors、1既有用户指定大字号 warning。
- SDK仍是 `gradle/libs.versions.toml` 的6.2.8；知识选择为6.2但 graph.json 缺失，本次以项目证据、技能内参考和精确SDK源码/编译验证为依据，没有使用知识图谱结论。
- 头显安装和启动成功，`app info`回读 versionName=1.0.24、versionCode=25、pid=10481。13:29:54.323首轮 Ready，23片花瓣，completionEnabled=false。
- 修复前后日志：`backups/replay-hit-surface-v1-2026-11-17/before-device.log`、`after-device.log`（本地保留）。
- 独立APK：`backups/replay-hit-surface-v1-2026-11-17/Petaluck-1.0.24.apk`。
- SHA-256：`ffbc5edc1212e685a7d123a63fb86229f8ace2f66cbc31a9ac3039ea5517de2a`。

仍待头显：连续第二、第三轮分别验证 GazePinch 和 DirectPinch 抓取/拖动/释放、结束文字不闪旧内容、重玩按钮可用。已请求用户复测；用户的“OK”只按知悉处理，不记为验收通过。此次未修改用户已有的 PetalRain Editor 文件。
