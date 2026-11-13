# 花瓣雨自由三维朝向

2026-11-13，APK 1.0.10 / versionCode 11，替代 v2 始终朝向相机的粒子朝向。

三个原生 Editor ParticleComponent 的 `orientationMode` 从 BILLBOARD 改为 FREE，方向基准 `(0,1,0)`、出生朝向 variation=.65。关闭原来12–35°/秒的单轴旋转模块；启用 separateAxes 的 XYZ rotationCurve，每轴在两条曲线间随机采样。X/Z 用往返的平滑贝塞尔曲线模拟摇摆，Y 缓慢翻转；三发射器用不同相位/翻转幅度及5/6/7秒寿命，避免整齐同步。无需运行时逐帧改实体或计算相机方向。

Unlit、透明混合、双面渲染保留。渲染 primitive 的 SDK 枚举仍叫 BILLBOARD，表示轻量四边形；实际**朝向约束是 FREE**，不会追随 camera。它仍是平面花瓣，侧向会缩窄为细边，不是有厚度的曲面网格。原来的当前轮三层花色贴图、微风、每片1秒淡入/3–5秒保持/1秒淡出、音乐联动均保留。

## Editor 与交付

- 原生 MCP 修改、save_current 成功，重新打包 `Scenes/PetalRain`，仅包含正式场景，未包含用户 `Hi.usda`。保存的 USDA 中三个 orientationMode 均为 FREE。
- 保存后参数回读见 `petal-rain-v3-editor-readback.json`：FREE、双面Unlit、XYZ随机曲线、X/Z转向反转及原有渐隐检查通过。旧常速旋转及 velocityCurve 均关闭，避免叠加。
- 暂停同一批原生粒子后，从 +Z 正面和 +X 侧面查看；`art/PetalRain/rotation-v3-front.png` / `rotation-v3-side.png` 已实际检查，能看到宽面、斜面、细边的混合，不再全体正对当前相机。截图包含 Editor 辅助线；不能证明头显运动平滑度或视角舒适度。检查后恢复 Editor 播放。
- `petal-rain.bundle` 14,871 bytes，SHA-256 `7af798983236ca4cbe2d0bee0c46f34696f90bde2c1d36f83746e23e78b45ec5`。直接打包 `managed=false` / packageGate=pass，未声称托管Workflow批准。
- bundle及两个sidecar逐字节拷贝进APK，ZIP未压缩。继续保持应用 .60×.70×.40m 容器；sidecar静态粒子bounds不能代表运动包络。

## 验证

- 开发/测试APK构建通过，111主机单测零失败。实验API PASS；SpatialUI 0 errors / 1既有大字warning；A11Y源码审核0 errors/warnings。资源参数编辑未增加镜像式单测。
- PICO `PB311XHGKC150135G` 已安装1.0.10/code11，进程25056。实际日志09:43:02.377：`PetalRainReady round=1 color=2 emitters=3 revision=free-orientation-v3`，绑定本轮三层纹理成功。
- 第一轮26片，公开click先摘petal0再顺序batch25片全部受理，实际日志确认09:44:48.320 `PetalRainStarted round=1` / `EndingMusicBlessing round=1`，09:45:02.828 `PetalRainStopped instanceDestroyed=true` / `EndingMusicStopped`，间隔14.508秒，支持自然结束清理。
- 重玩后第二轮color0、17片，09:45:29.363预加载了三张color0纹理，验证换色重新准备。此次日志证据替代v2未到首帧的限制，但不等于视觉验收。
- 第二轮同样自然结束（09:46:24.087–09:46:38.605）。第三轮color6、20片，09:48:42.732开始播放；播放中公开重玩动作受理后，09:48:45.958粒子销毁与 `EndingMusicFadeOut durationMs=1000` 同时记录，09:48:46.966音乐停止。第四轮color7正常预加载。限定原始日志在 `petal-rain-v3-device.log`；第三轮batch operationId `50f612fe-c071-4d58-88b1-2e92a9cee064`、重玩 `677c0681-679a-4ac7-92a3-9aca76374d74`，均performed=true，并由业务日志交叉验证。先前第二轮的重玩已晚于自然结束，未把它记作fade测试。
- 检查窗口crash buffer为空。保留第四轮Ready状态供用户体验；未要求或改变系统音量。
- 1920×1080设备空间截图已检查，应用主体在画面左侧之外且已过音乐结束时刻；仅存 `.scratch/petal-rain-v3-device-offscreen.png`，不作为粒子播放证据。未启用相机注入或录屏。头显连续翻转外观、实际渐隐、全色和性能仍待佩戴验证。

## APK

`backups/petal-rain-rotation-v3-2026-11-13/RoseTear-1.0.10.apk`，51,347,383 bytes。
SHA-256 `ddd531f4f33e89562c8f04dc55eda21c001542c9a2cc518edee9f7e6e9182cc7`。
