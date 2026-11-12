# PetalRain — Spatial Editor 花瓣雨

独立 Editor 6.2.4 工程，场景由原生 Editor MCP 创建、配置和保存。
打开 `PetalRain.spatialproject`，当前场景为 `Sources/Scenes/PetalRain.usda`。

## 交付

- `Build/petal-rain.bundle`：14,182 bytes。
- `Build/petal-rain.scenes.json`：Editor 导出的场景清单。
- `Build/petal-rain.provenance.json`：直接打包记录。
- 加载场景键：`Scenes/PetalRain`，以 sidecar 为准。
- SHA-256：`b06853e011a307471dde8133fd0de386e226b07c9cf5a2bf129b481599fc17e9`。

## 效果

三个自动播放、循环发射的 ParticleComponent：`PetalRain5/6/7`。
各 4 片/秒，总计 12 片/秒；各最多 32 片，总上限 96。
寿命分别为 5/6/7 秒，alpha 各淡入 1 秒、淡出 1 秒。
发射器位置 `(0, 0.33, 0)` 米，BOX 区域 `0.50 × 0.001 × 0.28` 米。
初速 0.075–0.09 米/秒、向下发射、2° 随机偏角、弱噪声漂移。
15–25 mm 花瓣，随机初始旋转、12–35°/秒旋转；无碰撞、无交互。
Billboard、Unlit、透明混合，白色材质乘子。

本次是粉色简易效果，使用新绘制的透明矢量花瓣精灵，源文件 `petal.svg`。
`tools/create_petal_rain_texture.py` 可重复生成 PNG。
未改正式花瓣贴图，未实现八色切换、音乐联动或接入 Android 应用。

## 验证与限制

- `preview-front.png` 和 `preview-angle.png` 是 Editor 实际视口截图：检查了正面分布、透明轮廓及斜侧面的深度分布；包含 Editor 辅助线。
- Editor 保存成功；正式 pack 完成；归档可读，含 scene、texture 数据，scene 数据中含 ParticleComponent 及三个发射器名称。
- provenance 的 `packageGate.status=pass`；`managed=false`，没有托管 Workflow 审批记录。
- 实验 API 检查 PASS；SpatialUI 样式检查 PASS（0 errors / 1 既有大字 warning），本次未改 Kotlin/UI。
- **容器限制**：Editor 导出的粒子实体 `bounds=null`，自动拟合仅得到 `0.256 × 0.256 × 0.256` 米，不能代表运动包络。保留原始 sidecar，没有伪造边界。接入现有应用时应保持其 `0.60 × 0.70 × 0.40` 米 DefaultWindowContainer，勿按 sidecar 缩小窗口。此范围是制作目标，噪声轨迹的严格包络尚未验证。
- 尚未验证 SDK 6.2.8 头显加载、完整时序渐隐、透明排序、实际裁切或性能。截图和归档检查不代表设备验证。

原有 `docs/petal-rain-plan.md` 保留；其中历史权限阻塞不再代表本次 Editor 制作状态，其应用联动方案仍待单独实施。
