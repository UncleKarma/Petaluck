# 全部花瓣边缘平滑

2026-11-17，用户要求外、中、内三层全部平滑。开发版 **1.0.25 / code26**，资产库 `rose-llm-v10`；包含上一独立提交 `95872b9` 的[外层轻微卷曲](outer-curl-v1.md)。

## 结果与预算

按原来的解析曲面重新采样，将长度/宽度的分段分别加倍。12种花瓣仍保留各自的不对称轮廓、浅缺口、薄闭壳、两套UV和原有全部顶点；中内层不新增卷曲。外层附加卷曲采用2.5mm回卷、1.2mm侧缘浅兜系数，70%长度处的横截面折转角仍小于同编号的中层，再小于内层。

| 模型 | 修改前三角面 | 修改后三角面 |
| --- | ---: | ---: |
| 外层，每种 | 412 | 1496 |
| 中层，每种 | 412 | 1496 |
| 内层，每种 | 476 | 1736 |
| 花芯＋茎 | 4842 | 4842 |

采样30瓣实际组合为51,402面；按30片全部取最大单片的保守上界为56,922面。用户本次增加三角面的要求覆盖旧PRD500/20,000面限制，生成器和验证器改用单瓣1,800、整花60,000上限，茎仍5,000。12个GLB合计增加356,484字节。所有PNG和stem.glb与修改前字节一致，24张花瓣纹理、布局、材质绑定和交互逻辑未改；精确网格碰撞体会随新GLB增加面数，头显性能尚未测量。

## 保留根部安装曲面

全域解析加密的初次检查，在N=21、seed=15的middle_03/inner_02安装附近触发了保守SAT越界报告。最终第一段（长度1/7）的新增顶点使用原始三角形重心插值，不移动既有安装曲面；下一段使用smoothstep过渡到解析曲面。该具体回归布局已通过，未扩大根部接触允许区域。

## 验证

- `verify_rose_assets.py`：实际GLB闭壳、UV、纹理、骨骼/花芯/茎和新三角面预算通过。
- `review_petal_refinement.py`：12片全部原有顶点精确保留；无退化面；相对更密的曲面参考，主要边缘最大弦误差由0.35–0.58mm降到0.12–0.21mm，逐片下降约51–73%。根部保留原来的分片曲面，短安装底边不计入这项可见边缘误差指标。见 [measurement](petal-smooth-edges-v1-measurement.json)。
- `verify_rose_presentation.py`：302布局、1510个入场包围盒通过；见 [presentation](rose-entrance-v1-verification.json)。
- 完整302布局/82,323对花瓣的最终SAT复查：通过，无根部允许区域外的主体穿插。
- `testDebugUnitTest assembleDebug assembleDebugAndroidTest`：成功；现有139项主机测试结果通过（未改Kotlin逻辑，Gradle复用现有测试结果）。实验API检查PASS；SpatialUI 0 errors / 1既有用户指定大字warning。
- 复读APK内12个GLB，均与本次生产资产字节一致。1.0.25/code26安装成功，app info确认运行pid21527；15:42:01启动后容器曾隐藏；15:43:04恢复首个场景帧后，12个花瓣GLB均导入成功，但在花茎处触发“Stem must have exactly one skinned mesh”。日志显示native loader6.4.1同时给ROOT和Stem_Skinned建立动画组件。花茎源文件/打包内容未变，需单独修正运行时选取并重新验收；本APK不作为原生可玩性通过证据。

待头显验收：外层卷曲是否足够自然、所有花瓣近看/持有时的轮廓、正反面/侧缘、加载和拖拽性能，以及先前仍待复测的多轮输入和完成闪帧。

## 预览与产物

- [12种花瓣修改前后](../art/petal-smooth-edges-v1-review.png)：实际GLB、离线诊断光照。
- [同一24瓣整花前/斜/侧对比](../art/rose-petal-refinement-v1-review.png)：生产Kotlin布局、实际GLB及纹理、离线Unlit。两张均不是头显截图。
- 本地快照/日志：`backups/petal-shape-v9-2026-11-17/`，`before`是修改前资产，`curled-low-poly`是第一项独立提交后的资产。
- APK：`backups/petal-shape-v9-2026-11-17/Petaluck-1.0.25.apk`。
- SHA-256：`fe3eeeabc94a71f9a26b81ae9d715174ea3e5fa742f1d301a349ee3373322537`。

复现比较：`python3 tools/review_petal_refinement.py --before backups/petal-shape-v9-2026-11-17/before --curled-before backups/petal-shape-v9-2026-11-17/curled-low-poly`。仅生成器输出写入生产assets，离线比较脚本不改生产资产。
