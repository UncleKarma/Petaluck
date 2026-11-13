# PetalRain — Spatial Editor 花瓣雨

独立 Editor 6.2.4 工程，场景由原生 Editor MCP 创建、配置和保存。
打开 `PetalRain.spatialproject`，当前场景为 `Sources/Scenes/PetalRain.usda`。

## 交付

- `Build/petal-rain.bundle`：15,204 bytes（2026-11-13 随机初始角度v4）。
- `Build/petal-rain.scenes.json`：Editor 导出的场景清单。
- `Build/petal-rain.provenance.json`：直接打包记录。
- 加载场景键：`Scenes/PetalRain`，以 sidecar 为准。
- SHA-256：`f5647755b773dbd6b388e717ae30c6140eb1e78fd4e8c563699c46b1d4348a4a`。

## 效果

三个自动播放、循环发射的 ParticleComponent：`PetalRain5/6/7`。
各 4 片/秒，总计 12 片/秒；各最多 32 片，总上限 96。
寿命分别为 5/6/7 秒，alpha 各淡入 1 秒、淡出 1 秒。
发射器位置 `(0, 0.33, 0)` 米，BOX 区域 `0.50 × 0.001 × 0.28` 米。
初速 0.075–0.09 米/秒、向下发射、2° 随机偏角、弱噪声漂移。
15–25 mm 花瓣，FREE方向(0,0,1)、variation 1，XYZ初始角均为0–360°随机常量，角速度X12–26/Y8–18/Z10–22°/秒；旧单轴angularSpeed模块关闭并归零。无碰撞、无交互。
轻量四边形粒子（primitive枚举BILLBOARD，orientationMode为FREE）、双面Unlit、透明混合，白色材质乘子；不追随camera。

本次是粉色简易效果，使用新绘制的透明矢量花瓣精灵，源文件 `petal.svg`。
`tools/create_petal_rain_texture.py` 可重复生成 PNG。
未改正式花瓣贴图；独立工程没有八色切换。后续 Android 1.0.8 的音乐联动接入见 `docs/ending-petal-rain-v1.md`，与本工程独立交付时的验证范围分开记录。

Android 1.0.9 在加载此包后，将三个发射器的贴图替换为本轮月季 outer/middle/inner albedo 绘制的透明花瓣，并按轮次 seed 变化微风；包本身与独立 Editor 预览不变。保留 Billboard/Unlit、1+3/4/5+1秒生命周期及随机旋转。设备 Canvas 24贴图检查通过；原生动态效果仍待验。见 `docs/ending-petal-rain-v2.md`。

2026-11-13 v3更新已打入Android1.0.10：取消朝向camera，正面/侧面暂停视图 `rotation-v3-front.png` / `rotation-v3-side.png` 已查看，存在宽面/斜面/细边。原生日志验证播放和自然结束停止；连续运动观感仍待佩戴验证。详情 `docs/ending-petal-rain-v3.md`。以下旧验证记录保留历史范围。

## 验证与限制

当前v4使用原生`pack_project`在现有GUI会话打包，manifest由Editor导出，runtimeSceneKey从包内AssetInfo验证补充；provenance是调用方记录的native receipt，不是CLI/托管gate。保存USDA已检查；脚本重开场景被Editor安全策略拒绝，未声称重载验证。`rotation-v4-front.png`与`rotation-v4-side.png`均为已检查的最终设置截图，参数见`docs/petal-rain-v4-editor-readback.json`。111单测与构建通过，1.0.11设备日志确认3组同花色粒子准备完成，连续姿态仍待佩戴验证。下列打包gate说明仅适用于早期版本。

- `preview-front.png` 和 `preview-angle.png` 是 Editor 实际视口截图：检查了正面分布、透明轮廓及斜侧面的深度分布；包含 Editor 辅助线。
- Editor 保存成功；正式 pack 完成；归档可读，含 scene、texture 数据，scene 数据中含 ParticleComponent 及三个发射器名称。
- provenance 的 `packageGate.status=pass`；`managed=false`，没有托管 Workflow 审批记录。
- 实验 API 检查 PASS；SpatialUI 样式检查 PASS（0 errors / 1 既有大字 warning），本次未改 Kotlin/UI。
- **容器限制**：Editor 导出的粒子实体 `bounds=null`，自动拟合仅得到 `0.256 × 0.256 × 0.256` 米，不能代表运动包络。保留原始 sidecar，没有伪造边界。接入现有应用时应保持其 `0.60 × 0.70 × 0.40` 米 DefaultWindowContainer，勿按 sidecar 缩小窗口。此范围是制作目标，噪声轨迹的严格包络尚未验证。
- 尚未验证 SDK 6.2.8 头显加载、完整时序渐隐、透明排序、实际裁切或性能。截图和归档检查不代表设备验证。

原有 `docs/petal-rain-plan.md` 保留；其中历史权限阻塞不再代表本次 Editor 制作状态，其应用联动方案仍待单独实施。
