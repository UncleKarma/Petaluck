# 同花色花瓣雨

2026-11-13，APK 1.0.9 / versionCode 10。

## 行为

继续加载 Editor 的 `petal-rain.bundle`，场景键 `Scenes/PetalRain`。原生 Editor 回读确认三个发射器的 Billboard、DEFAULT_UNLIT、TRANSPARENT、随机初始旋转 0–360°、旋转速度 12–35°/秒、噪声及 alpha 曲线已经符合要求，故保留已验证包及两个 sidecar 原字节。回读在 `petal-rain-v2-editor-readback.json`；独立 Editor 预览仍为粉色样例。

每轮 Ready 后把 `RoundConfig` 传入粒子准备任务，通过正式 `RoseLayout.texturePath` 分别取当前 colorId 的 outer/middle/inner 纹理。`PetalRainSprite` 用 Android Canvas 的 BitmapShader 将原始 albedo 绘入既有四段贝塞尔花瓣轮廓，得到三张 256×256 RGBA 精灵。保留源图局部色素、纹理和三层差别，无色相覆盖，无粉色底图乘色；正式花朵纹理不改动。sRGB、mipmap 全级；原 Bitmap 在上传后回收。

三个 `mainSimulation.material.baseColorMap` 在启动前替换为上述纹理。最多保留当前花色的三张，使用 `toGlobal()` 固定原生寿命，同色可重用，换色在旧实体销毁后释放。清理和不可取消加载沿用 epoch/closed/caller 校验，销毁过期实体；结束场景等加载结果收回后再释放纹理与 bundle。准备失败只记日志，不影响音乐或花朵状态。

每片生命周期依发射器为 5/6/7 秒，即 **1 秒淡入、3/4/5 秒保持、1 秒淡出**，并非整场花瓣雨只播放一次 5–7 秒。音乐持续期间按合计 12 片/秒循环发射，顶部 `(0,.33,0)m`。音乐自然结束、重玩淡出开始、暂停或 teardown 仍立即 disable/destroy，清掉存活粒子。

每轮用独立的 `Random(config.seed xor 0x504554414C)`，为三个 Perlin 场分别抽取 seed、强度 .00045–.00075、尺度 .12–.16、演进速度 .2–.3；不消耗游戏布局/答案随机流。保留包中每片随机方向角与速度，形成缓慢旋转和微风变化。Billboard 保持朝向观察者；它是平面旋转，不是实体花瓣翻面模拟。实际微风幅度和裁切仍需头显动态观察。

## 验证

- 111 项主机单测：0 failures/errors/skipped；开发 APK 和 Android 测试 APK 构建通过。
- PICO `PB311XHGKC150135G` 实际运行 `PetalRainSpriteTest`，`OK (1 test)` / 0.808s。遍历全部 24 张生产 albedo，独立矩形绘制参考与花瓣内部 RGB 每通道差≤2，内部 alpha=255、外部=0、有抗锯齿半透明边缘，24 个输出均不同。
- 设备测试输出 `art/petal-rain-colors-v2.png` 已取回并查看：行按 colorId 0–7，列 outer/middle/inner，白色为3、蓝色为4。该图是实际 Android Canvas 输出，**不是原生粒子播放截图**。
- Editor 回读计算确认每个淡入、淡出均为1秒，保持为3/4/5秒；Billboard/Unlit/rotation/noise开启。
- 实验 API 检查 PASS；SpatialUI 检查 PASS，0 errors / 1 既有大字 warning。A11Y源码审核0 errors/warnings，保留人工完成的 petal0 绑定，仅刷新代码变更后的来源锚点。
- APK bundle 未压缩；包和两个 sidecar 与 Editor Build 逐字节一致。
- 1.0.9 / code10 已安装并启动，设备包信息确认。新进程18240日志显示 onPause/onStop、容器 `v=false`、SpatialViewHide，未收到应用场景首帧，故本轮尚未验证原生纹理绑定、动态雨、渐隐、自然结束/重玩时同步或性能。旧1.0.8预加载日志不能作为新版成功证据；本次crash buffer为空仅限检查窗口。未启用相机注入或录屏。

## 安装包

`backups/petal-rain-colors-v2-2026-11-13/RoseTear-1.0.9.apk`，51,346,695 bytes。

SHA-256：`4897abd167b75bb9111a36e1c4231b12419d06b030b3d015759f18b87669d0fd`。
