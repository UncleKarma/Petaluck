# 重玩文字闪帧修复

版本：1.0.6 / versionCode 7。修改前提交：`536dfd7`。

## 源码证据

原 `newRound()` 发布 Loading 时，`syncPanels()` 立即将同一个
`rose_message` 从完成位置 `(0,0,.12)` 移至 `(0,-.245,.075)` 米。
Compose 需要之后的重组、布局和绘制才会将祝福/按钮替换为加载文字，
存在旧内容被显示在新位置的窗口。原结果文字也依赖 `LaunchedEffect`
才能清空，在重开后的首次重组中可能保留上一轮文字。
这是源码确认的时序竞态，尚未通过头显逐帧录制确认全部可见因素。

## 修订

- 三个附件始终保持各自位置；完成祝福和原生重玩按钮仍在同一个布局，
  独立为 `rose_completion`，普通提示/错误重试保留 `rose_message`。
- 原生表面宽 560dp，高分别为 result 104dp、message 128dp、
  completion 384dp；完成组在固定表面中居中，原字号、按钮间距和淡入保留。
  阶段切换不再移动或缩放旧文字所在的表面。
- 重开同步禁用完成面板和空结果面板，避免旧缓冲残留以及透明命中区域。
  结果文本按 roundId 重置，并在 result 为空时直接输出空字符串。
- 三附件均在首个真实场景帧之后创建；绑定齐全才加载模型；
  保留重复点击防护、错误重试配置、生命周期清理及 native Button。
- 无障碍合同增加 `rose.completion`，保留 `rose.petal.0` 工厂绑定。

## 验证与边界

- 99 项主机单测通过，0 failures/errors/skipped。
- `testDebugUnitTest assembleDebug assembleDebugAndroidTest` 成功。
- APK 已安装到连接的 PICO 头显，设备查询确认 versionCode=7 / versionName=1.0.6。
- `LaunchSmokeTest` 执行失败：20 秒内可见文字集合为空（observed=[]）。
  随后 `dumpsys power` 显示 `mWakefulness=Asleep`；日志中无本次场景启动证据。
  此检查未通过，不能据此确认启动正常，也不能单凭它判定为应用回归；需佩戴后复测。
- 本轮未找到此前使用的实验 opt-in 和 SpatialUI 检查器，
  不以历史检查结果冒充本轮 PASS。当前修改使用现有 SpatialUI 控件与公开 Entity.enabled。
- 闪帧、完成组位置/文字裁切、手势按钮命中、错误重试以及暂停恢复需头显复测。
  增加一个常驻附件，原生表面资源开销尚未测量。

APK 独立归档于 `backups/replay-panel-v1-2026-11-12/`，附 SHA-256。
