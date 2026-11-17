# 花茎蒙皮渲染节点筛选

2026-11-17，开发版 **1.0.26 / code27**。承接 `95872b9` 外层卷曲和 `b0a01bd` 全部花瓣边缘平滑，生产资产保持 `rose-llm-v10`。

## 原生故障与修正边界

1.0.25在头显恢复首个场景帧后，12个新花瓣GLB导入成功，随后因 `findSkinnedMeshEntity().singleOrNull()` 不满足唯一性而进入Error。native loader日志为6.4.1，并分别给ROOT和Stem_Skinned建立AnimGraphComponent；项目BOM仍为6.2.8，花茎源文件和APK内容未变。

旧错误没有记录候选列表，无法仅凭两个AnimGraph日志断言它当时返回了两个节点，也不能排除启动时序或空列表。这次保护性修正按Entity.id去重，排除没有ModelComponent的动画/根owner，再严格要求剩下恰好一个、名称为Stem_Skinned的节点。将完整候选列表写入日志，实际缺失、多个渲染网格或名称不符仍走Error；没有任取第一个、替代花茎或隐藏失败。后续仍使用原来的三骨骼名称/数量校验和实际场景帧就绪等待。

## 验证

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest`成功，139项测试、0失败/错误/跳过。
- 实验API checker通过；SpatialUI 0 errors / 1既有大字号warning。
- PB311XHGKC150135G（B3110/swan、真实设备、Android16/API36）安装并启动成功，app info回读1.0.26/code27，pid13881。
- 16:45:19.293：`StemSkinSelected owner=24 candidates=[Stem_Skinned:24:model=true]`。本次只有一个候选，证实选中正确节点，但不证明已经复现并排除了之前的唯一性失败。
- 16:45:19.359：首轮22片、color4装配成功；16:45:20.167入场完成并启用交互，`completionEnabled=false`。
- 空间FOV截图为可读1920×1080 PNG，蓝色花朵可见。花较小且与其他窗口重叠；此证据仅确认原生显示，不作为近距离边缘、完整眼手行为或帧率验收。

本地证据：`backups/petal-shape-v9-2026-11-17/device-stem-load-failure.log`、`device-1.0.26.log`、`device-1.0.26.png`。后续若重现装配失败，候选列表可区分空列表、非模型owner与真实重复网格。

最终APK：`backups/petal-shape-v9-2026-11-17/Petaluck-1.0.26.apk`；SHA-256 `3cdf2c94e73d7e2d977b71eb9be1c5d129b5b02871da410598775292f92c8e7b`。1.0.25 APK及其失败记录单独保留。头显近看卷曲与边缘、多轮Gaze/Direct输入和性能仍待用户实测。
