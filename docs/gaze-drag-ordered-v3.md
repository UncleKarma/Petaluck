# GazePinch 有序增量修订 / 1.0.4

日期：2026-11-12。包名 `com.example.flowertears.p18ab52cf`，versionCode 5。

## 范围与行为

本次处理用户反馈的注视捏合花茎随动与脱落跳位问题。DirectPinch 的局部指针绝对位移路径、花茎 XZ 弯曲规则、4 cm 任意方向摘取、模型、UI 和音效增益保持。此版是待头显复测的修订，不代表原生坐标问题已确认解决。

- `PinchDragTracker.update`：remote 只将当前 raw pose 减去上一有效 raw pose，按该事件的映射翻 Y、换算至场景米。`frameDelta` 是本笔位移，`delta` 是这些已换算位移的累计净向量。换算失败不推进上一采样。
- `RemotePetalDrag.start/moveBy/advance/clear`：从开始拉扯到摘下后持有，全程排队相邻位移，场景帧按事件顺序消费。脱落前由净向量长度判断 8 mm 死区与 4 cm 阈值；不能先合并同一帧的所有样本，否则“越界后拉回”会漏摘。
- 跨阈值时求解 `|D + t·delta| = 0.04`，先用阈值前一段驱动受力和花瓣。`RoseScene.detach` 保存完整 root-relative TRS，换父后恢复同一 TRS，并把这份快照直接返回。快照位置用于初始化持有状态，余下的 `(1-t)·delta` 只应用一次。
- 后续持有运动继续通过 `IncrementalPetalDrag` 累加；不限制距离、不乘 dt、不重算抓取起点、不继承花冠回弹。
- release 先消费全部待处理事件，再记录 fallStart；即使 tracker 已处理 up 并清空 capture，独立的 remote 状态仍然存在。最后一瓣的状态顺序与清理保持。

## 缺陷分析与验证边界

1. **已由旧代码失败用例证实的历史位移重算**：raw 从 1000 到 1010，倍率 0.001 得到 1 cm；手停在 1010，倍率变成 0.003 时，旧 `delta` 被重算成 3 cm。新增测试在旧实现断言失败；改为逐笔换算累计后通过。这证明该合成输入下的逻辑问题，不证明头显实际发生了倍率变化。
2. **静态确认的跨阈值更新顺序**：旧场景先应用整笔拉扯位移再 detach。新分段算法的测试确认阈值处截取姿态，并仅将余量交给持有运动。原生 `RoseScene` 的骨骼更新、矩阵缓存及屏幕可见连续性未在 JVM 测试中执行。
3. **仍待确认的设备输入契约**：SDK 6.2.8 的 `convertSize` 只做比例换算；本轮没有证据支持改动 rawPosition 单位或增加新的倍率。现有翻 Y/单位映射保留。如果设备或换算输出一笔 30 cm 位移，新算法仍保留这笔总运动，不会通过限幅隐藏它，因此还可能出现明显跳动。
4. 花茎当前只根据 X/Z 弯曲，纯 Y 拖动可以摘下但不会产生横向弯曲。此行为两种 pinch 共用，本轮未扩大修改。

## 生成的用例与结果

测试工具流程：Step1 prepare 完成；Step2 Kotlin/Android/Gradle 上下文完成，EXEC_SOURCE 为空；Step3–4 范围及缺陷分析完成；Step5 生成、验证、修复完成；Step6 按无覆盖率要求/配置跳过；Step7 尝试 `utree flush`，工具路径不存在（exit 127），报告保存在此文件。

- 新增 10 个测试：1 个倍率变化时静止手的回归；9 个有序拖拽测试，覆盖慢速净位移、往返抖动、4 cm 六轴边界、斜向相交、跨过原点反拉、同帧越界回退、完整保留大位移、空帧不重复、最后一瓣无帧 release、失效输入及清理隔离（部分用例覆盖多个场景）。
- 维护 2 处原有断言以匹配 remote 新契约：相邻采样的换算调用序列、倍率变化后的累计净位移。
- 首轮新倍率回归在旧 tracker 上失败；随后构建发现 SDK 不提供 `getPosition()` 函数。最终交接改为直接返回保存的 Transform，避免额外读取位置。tracker 测试类、有序拖拽测试类分别通过；最后全量 **96 tests / 0 failures / 0 errors / 0 skipped**。
- 执行 `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest` 成功。测试 APK 仅编译，没有执行设备测试。
- 实验 API 检查 PASS；SpatialUI 样式 PASS，0 errors / 1 条既有大字号提示。
- 原始和 raw WAV SHA-256 均为 `ea4338224bc3db7a417aade1cacc897069c9f3ae94302c2ebac433e269368982`；PCM gain 0.025、track gain 1 保持。

## APK

`backups/gaze-drag-ordered-v3-2026-11-12/RoseTear-1.0.4-gaze-drag-ordered-v3.apk`

SHA-256：`f70f86de48bf772e0c36e5531cafbbfad0170d3ffbb7254d23f2cc35c84d4709`

源码以 Git 初始提交 `5879919` 为本轮基线；旧版 APK 和快照保留。本修订对应后续独立提交，以 `git log` 实际记录为准。

## 头显复测与日志

本会话未安装/启动 APK，也未采集头显输入。既有 ADB listener 权限限制不绕过。用户可以在本机终端连接设备后运行：

```bash
adb install -r backups/gaze-drag-ordered-v3-2026-11-12/RoseTear-1.0.4-gaze-drag-ordered-v3.apk
adb shell setprop log.tag.RoseGaze DEBUG
adb logcat -v threadtime RoseGaze:D FlowerTears:I '*:S' > gaze-ordered-v3.log
```

复现后 Ctrl-C 停止采集；`adb shell setprop log.tag.RoseGaze INFO` 关闭详细日志。没有清空设备原有日志。

`RoseGaze` 的每条记录带 `revision=ordered-v3` 与 session。DEBUG 记录 rawPrevious/rawCurrent/rawDelta/sceneDelta、排队总量、pull 前后姿态与 strength、held position、release fallStart；INFO 每次摘下记录 threshold pull 与 reparent 前后完整 TRS。

优先复测：慢速横向拉扯能否带动花茎；接近 4 cm 时脱落是否连续；摘下后停手、往返与松手；最后一瓣；暂停/恢复；DirectPinch 回归。若仍飞远，对照 rawDelta → sceneDelta → loaded → reparent → held 的数值，区分设备跳变、比例异常、原生姿态更新与持有运动。真实米制标定、骨骼显示及手眼交互结果需设备证据。
