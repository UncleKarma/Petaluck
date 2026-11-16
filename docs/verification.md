# 验证记录与继续工作

## 彩色月季跨层邻接 1.0.19

增加所有层组合的相近方位异色约束，左右邻接保留。136测试与构建/静态检查通过；已安装，白色月季 Ready 场景审核通过，彩色原生观感待复测。详见 [multicolor-neighbors-v2.md](multicolor-neighbors-v2.md)。


## 彩色月季 1.0.18

新增第九种等概率花色、逐瓣随机基础色和八色花瓣雨；135测试、构建和静态检查通过，当前无连接设备，原生效果待验证。详见 [multicolor-v1.md](multicolor-v1.md)。


## 英文显示名 Petaluck

2026-11-16：默认资源 `app_name` 改为 `Petaluck`，中文简体和繁体资源仍为「一瓣欢喜」。`assembleDebug` 通过；AAPT2 核对 APK 默认/英文标签为 `Petaluck`、中文各地区为「一瓣欢喜」，包名和 1.0.17/code18 保持。实验 API 检查通过；SpatialUI 0 errors、1 条既有的大字号 warning。本节只记录本次名称变更；旧版 APK 的历史名称记录保留。

## 第二轮结束闪帧（buffer-gate-v2）

1.0.17/code18补上重玩空白提交后停用面板，并将提交回调绑定到实际空白绘制。132单测、构建、API/样式/A11Y和bundle检查通过，已安装；连续第二/第三轮头显验证尚未完成，详见 [completion-buffer-v2.md](completion-buffer-v2.md)。


日期：2026-11-13。输入为 PRD v1.8（`requirements/flower-tears-prd.md`）。本交付为可构建开发版，尚未完成全部 P0 真机验收。

## 花瓣雨自然收尾（当前，drain-tail-v5）

- 1.0.12 / code13：旋转统一加快25%，出生方向偏向尖端朝上/朝侧；每片5秒，10秒停止发射、15秒清理，14.5秒音乐自然结束保留尾段。
- 118单测与开发/测试APK构建通过；实验API、SpatialUI、包CRC/场景键/未压缩字节核验通过；原生Editor正面/斜侧面已查看。
- 已安装；设备唤醒后花朵装配报`Stem must have exactly one skinned mesh`，重启后公开操作节点不可用，未完成本版原生播放验证。该花茎资产/加载断言未在本轮改动，原因待定位。详见 [ending-petal-rain-v5.md](ending-petal-rain-v5.md)。

## 自由朝向花瓣雨（历史，ending-petal-rain-v3）

- APK1.0.10 / code11：Editor粒子FREE朝向、双面Unlit、随机XYZ曲线摇摆翻转，替代camera-facing和单轴匀速旋转；已重新打包并安装。
- Editor暂停同批粒子做正面/侧面检查，看到不同倾斜与细边；参数及保存文件检查通过。111单测、构建、实验API、SpatialUI、A11Y及APK包完整性检查通过。
- 真机确认两轮音乐开始时启动、约14.5秒后自然停止销毁；第三轮播放中重玩，粒子与1000ms音乐fade开始同刻停止，音乐约1秒后结束；第四轮正常预加载。头显连续运动观感仍待验证；当前空间截图主体离开画面，不能作动态效果验收。见 [ending-petal-rain-v3.md](ending-petal-rain-v3.md)。

## 同花色花瓣雨（ending-petal-rain-v2）

- APK1.0.9 / code10：粒子直接使用本轮三层albedo，透明花瓣轮廓、Billboard/Unlit、1秒淡入+3/4/5秒保持+1秒淡出；每轮独立微风随机流。沿用现有音乐联动和停止销毁。
- 111单测、开发/测试APK、实验API、SpatialUI及bundle完整性检查通过；设备测试遍历24张生产贴图，颜色/alpha/边缘/独立性检查PASS，检查图已查看。源码A11Y审核0 errors/warnings。
- 已安装1.0.9，启动后原生容器隐藏，未到场景首帧，本版原生预加载/动态播放仍待验证。记录、图及APK哈希见 [ending-petal-rain-v2.md](ending-petal-rain-v2.md)。

## 结束音乐花瓣雨（ending-petal-rain-v1）

- APK 1.0.8 / code 9：bundle 的三发射器在音乐成功 play 后启动，直接挂 SpatialView、顶部 y=.33m；自然结束/超时/暂停/退出及重玩淡出开始清空粒子，旧 fade 结束不再次通知视觉停止。
- 111 项单测零失败；开发/仪器测试 APK 构建、实验 API、SpatialUI 检查通过；pico-cli 验证 1 个 bundle 使用 ZIP method 0，且字节与 Editor 包一致。
- 已升级安装到 PICO；实际日志出现 PetalRainReady round=1 emitters=3 y=0.33，随后 EndingMusicReady。Ready 阶段 A11Y 审核 0 errors；全 Contract 单次审核因阶段隐藏面板失败，未伪称全阶段 PASS。
- 一次公开摘取动作成功；剩余 batch 第一步 TARGET_ROOT_TIMEOUT，0 步派发，未触发音乐。录屏停止、原相机与追踪恢复确认。启动及测试结束 crash buffer 均为空，不能据此声称全流程无崩溃或视觉验收。详情见 [ending-petal-rain-v1.md](ending-petal-rain-v1.md)。

## 祝福音乐与重玩淡出（ending-bgm-v1）

- APK 1.0.7 / code 8：祝福出现时单次播放 endingBGM；有效重玩以 1000 ms 淡出旧音乐，同时装配新花。自然结束停止，暂停/退出清理。
- 108/108 主机单测及开发/仪器测试 APK 构建通过；实验 API 检查 PASS，SpatialUI 0 errors / 1 既有 warning。用户 WAV 与 APK 原字节一致。
- 本轮未安装、未在头显试听；实际同步、音量、淡出和系统背景音共存待验证。见 [ending-bgm-v1.md](ending-bgm-v1.md)。

## 重玩文字闪帧（replay-panel-v1）

- APK 1.0.6 / code 7：普通提示与完成组固定在独立附件位置，固定表面尺寸；重开同步隐藏旧完成/结果面板，结果 remembered state 按轮次重置。
- 99 项单测和开发/测试 APK 构建通过；已安装并核对版本。启动仪器测试未读到任何可见文字而失败，随后确认头显处于 Asleep，不能声称启动验证通过。用户佩戴 1.0.6 实机点击“再试一次”复测，确认错位闪帧已消失（2026-11-12，目视确认，未逐帧录制；版式/命中/暂停恢复等其余项未逐项验证）。
- 实验 opt-in / SpatialUI 检查器未找到，本轮未声称 PASS；详见 [replay-panel-v1.md](replay-panel-v1.md)。

## Gaze 首移动基准与输入回调姿态更新（当前输入策略，move-anchor-v4）

- 实际 1.0.4 日志中 down 到首 MOVE 间隔 572 ms，差值换算约 38 cm，立即摘取并将约 34 cm 余量应用到持有花瓣；同帧写后的原生 loaded 姿态仍为旧值。
- APK 1.0.5 / code 6：remote 首有效 MOVE 只建基准，后续相邻采样按序在输入回调消费；4 cm 分段与 Direct 运动路径保持。99 项单测、开发/测试 APK 构建通过，新增 3 项基准/实测数值回归。
- ADB 安装 Success，已核对设备版本。用户复测反馈“看上去行为一致了”；4 次 Gaze 摘取均出现阈值前连续受力，首次 held 相对 tear 仅 0.397–1.136 mm，换父位置误差 < 0.00001 mm；Direct 对照位置连续。采集停止，详细日志等级已恢复。此次有限真机反馈不代表全场景验收。此前检查器脚本本轮未找到，未声称新的实验 API / SpatialUI verifier PASS。
- APK、旧日志及新日志单独保存在 `backups/gaze-move-anchor-v4-2026-11-12/`；详情见 [gaze-move-anchor-v4.md](gaze-move-anchor-v4.md)。

## GazePinch 全程有序增量与阈值分段（历史，ordered-v3）

- APK 1.0.4 / code 5。remote 从抓取开始累计相邻有效手部位移；跨 4 cm 分段，按完整 TRS 快照换父，剩余位移只应用一次。release 即使没有新场景帧也先消费队列；DirectPinch 路径保持。
- 新增 10 项回归，原倍率变化缺陷先在旧代码上失败，修订后测试通过。全量 96 项单测零失败，开发/测试 APK 构建成功；实验 API 与 SpatialUI 检查通过，保留既有一条大字号提示。
- 头显输入单位、骨骼/矩阵刷新与实际连续性待验证。异常大输入仍完整跟随，不以限幅掩盖；详见 `docs/gaze-drag-ordered-v3.md` 中复测与日志说明。
- APK 与 SHA-256 独立保存于 `backups/gaze-drag-ordered-v3-2026-11-12/`。Git 已存在，初始提交 `5879919`，下文“尚无 Git”的描述均为历史状态。本轮提交以实际 Git 记录为准。

## petalDrop 播放增益再降低 50%（历史 PCM v3，当前音效参数）

- 按用户追加要求，将 PCM gain 从 0.05 调整为 0.025，音轨 gain 保持 1；APK 1.0.3 / code 4，日志 revision=pcm-gain-v3。
- 当前原始 WAV/raw 字节保持；与 v2 快照相比，生产目录仅 `PetalAudioSamples.kt` 变化。实际工厂输出经逐样本独立检查，RMS 89.405863 → 44.700712，比例 0.499975，约 -6.02103 dB；原采样率、帧数和时长保持。
- 同步已有采样测试预期，未新增用例。针对性测试和全部 86 项单测均一轮通过；开发/测试 APK 构建、实验 API 与 SpatialUI 检查通过，既有大字号提示保留。
- APK 内原 WAV 字节一致且不压缩。输入信号测量不代表头显试听；未安装或测量头显输出。
- 新版源码、APK、清单与 SHA-256 保存在 `backups/petal-audio-pcm-v3-2026-11-12/`。Git 仓库仍未建立，没有 commit。utree flush 已尝试但工具缺失，详情记录在 `docs/petal-audio-pcm-v3-review.json`。

## petalDrop 播放增益再降低 50%（历史 PCM v2）

- 播放采样增益由 0.10 调整为 0.05，音轨 gain 仍为 1。APK 版本 1.0.2 / code 3，日志 revision=pcm-gain-v2。交互和场景代码未修改。
- 保留当前用户 WAV/raw 文件，SHA-256 为 `ea4338224bc3db7a417aade1cacc897069c9f3ae94302c2ebac433e269368982`。它含 LIST 元数据，已经不同于旧 v1 归档中的 WAV；本轮降低 50% 是相对于当前文件应用原 0.10 增益，不恢复旧文件。
- 生产工厂实际缓冲经独立逐样本复核：当前文件按旧增益处理的 RMS 为 178.839752，新值为 89.405863，比例 0.499922，约 -6.02196 dB；峰值 16384 → 819。48 kHz / 3600 帧 / 75 ms 保持。
- 维护已有音频测试的增益预期，并修正两处依赖固定 44 字节 WAV 头的测试；奇数 data 长度测试也改为定位实际 data 块。86 项单测零失败，开发及测试 APK 编译通过；实验 API 和 SpatialUI 检查通过，保留一条既有大字号提示。
- APK 内 WAV 与当前 raw 完全一致且不压缩。输入信号检查不代替头显试听，未执行设备安装或输出测量。
- 新版本独立保存于 `backups/petal-audio-pcm-v2-2026-11-12/`，详见 `docs/petal-audio-pcm-v2-review.json`。Git 仓库仍未建立，没有 commit。测试流程的 utree flush 已尝试但工具缺失，测试结果记录在本地报告中。

## 播放缓冲区直接衰减与音量验证（历史 v1）

- 用户反馈 `setVolume(0.1)` 后听起来仍与原来接近。之前只改音轨参数并完成主机检查，没有头显输出测量，因此不能确认 PICO 空间路径执行了该增益，也不能将用户反馈归因于听感非线性或旧包安装。标准 Android 文档说明 setVolume 是线性增益，0.1 对应预期约 -20 dB，并非听感音量 10%。原生忽略/覆盖增益等具体原因仍未知。
- 已核对上一交付 APK 的 SHA-256，并通过 DEX 反汇编确认 `const 0.1 -> AudioTrack.setVolume` 确实存在，见 `docs/petal-audio-pcm-v1-old-setter.txt`。这证明包内配置，不能证明设备实际执行或最终声压降低。
- 改为 `PetalAudioSamples.fromWav` 在加载时对原始 signed little-endian PCM16 每个样本乘 0.10 并重新编码，然后将这一缓冲写入空间 AudioTrack。音轨 gain 固定为 1，避免二次衰减。处理后的 clip 每个场景只缓存一次，重玩不重复缩小。原始 `petalDrop.wav` / raw 文件原样保留，不做归一化、不改系统音量；OBJECT 绑定、声音定位、可选加载、结束/暂停清理均保留。
- 新增 5 项回归：生产工厂真实缓冲的逐样本误差和峰值/RMS、PCM16 正负边界/字节序、零增益静音和单位增益无损且不改源缓冲、非法/放大增益拒绝、20 次准备不复合衰减。总计 86 项单测零失败，开发/测试 APK 编译通过；实验 API 检查 PASS，SpatialUI 样式 0 errors / 1 条既有大字号提示。
- 将实际生产工厂输出导出后，用独立 Python 重新测量：峰值 32767 -> 3277，RMS 3576.392571 -> 357.629038，变化 -20.000248 dB；48 kHz、3600 帧、75 ms 保持。可试听文件 `art/audio/petalDrop-pcm-010.wav` 由这份输出包 WAV 头生成，未再处理/归一化，见 `docs/petal-audio-pcm-v1-measurement.json`。这些数值证明送入音轨的数字信号衰减，不能代替头显输出测量。
- 新 APK DEX 核验确认实际路径包含 `PetalAudioSamples.fromWav -> PcmWaveClip.attenuated(0.1)`、写缓冲和 `AudioTrack.setVolume(1)`，见 `docs/petal-audio-pcm-v1-apk-path.txt`。Android 包版本升为 versionCode 2 / versionName 1.0.1，包名和签名路径保留，便于区分此次安装；没有确认用户头显当前安装版本。
- 本轮不修改拖拽、场景、花朵资产、UI 或无障碍契约。对照 v2 快照这些文件哈希一致，APK 原始 WAV/raw/source 字节一致且不压缩。旧 v2 对 DirectPinch/gaze 增量逻辑的设备待验状态保持，没有声称此次验证了交互。
- ADB listener 既有沙箱限制仍在，未重试被拒操作，未安装/启动或获取头显音频、焦点 dump、crash buffer。头显最终响度、空间听感及背景声混音待实测；不能将这次输入测量称为“PICO 增益问题已证实或设备已修好”。
- 独立快照 `backups/petal-audio-pcm-v1-2026-11-12/` 保留源码、APK、逐文件清单和 SHA-256；旧版不覆盖。Git 元数据仍受限制，没有 commit。详情见 `docs/petal-audio-pcm-v1-review.json`。

## 注视捏合脱落后逐帧累加、音量再降 60%（v2，音轨增益的听感未获确认）

- 用户确认 v1 的 gazePinch 仍不正确，并明确要求“脱落点不跳位，之后每帧手移动 delta，花瓣在当前位置加 delta”；DirectPinch 已正确，要求不改。上一版仅改了坐标原点处理，脱落后仍通过累计总位移重建位置，没有实现这次指定的逐帧状态更新。具体头显跳位原因没有输入轨迹，不能把原点或缩放解释当作已证实的原生根因。
- `PinchDragTracker` 给 remote 额外产生相邻有效原始手部采样的 `frameDelta`，使用该采样时的方向/单位转换。抓取起点总 delta 保留给脱落前拉取；原始上一采样只在有效转换后推进。源/目标/手和原点锁定、无 pose release、4 cm 阈值保留。
- `IncrementalPetalDrag` 仅供 remote/gaze 脱落后使用。detach 在 reparent 前采样的完整姿态中取位置初始化，pending 清零，不再次加入跨阈值事件的移动。之后的输入只排队 frameDelta；场景更新在花茎回弹之后执行 position += 本帧 pending delta，并一次清空。空帧或手静止加零，没有位移 cap，也不乘 dt，不依赖抓取起点/花芯姿态。
- release 在状态进入 Falling 和读取 fallStart 前，先应用尚未到场景帧的 pending delta，然后才清输入、开始原有 1.2 s 飘落/淡出。新 grab、释放、清轮均清增量状态；pause/stream loss/tracking timeout 沿用原释放和清理流程，末瓣长按仍到松手才结算。
- DirectPinch 继续走原有 `petalDrag.move(step.delta); pull(hand)`。与上一归档逐字对比，`pointerPosition`、`pointerDisplacement`、`pull` 和整个 `PetalDragMotion.kt` 均保持原样；direct 不计算 remote frameDelta，增量状态始终为空，不写其持有花瓣位置。已有 DirectPinch 行为单测仍通过。
- 音轨 gain 从 0.25 改为 0.10（0.25 × 40%，即再降低 60%）；其余音频实现保持原样。源 WAV/raw/APK 仍逐字节一致，不调系统音量，不申请焦点。
- 新增 6 项增量回归，更新既有 remote 原点测试以调用新生产模型：detach 与 90 个静止帧、多个输入在场景帧合并且只应用一次、三轴/反向大位移、release 前 flush/清轮无旧偏移、转换比例变化不重新应用总位移、无效采样恢复/metadata kind 改变、末瓣无 pose 松手后从 pending 的最终位置飘落。81 项单测零失败；开发 APK、测试 APK 编译通过。没有新建仅校验音量常量的镜像测试。
- 实验 API 检查 PASS，无新增 opt-in；SpatialUI 样式 0 errors / 1 条既有大字号提示；位置源码 0 violations / 2 条既有启发式提示并已处置。无障碍 ID、标签、动作与人工完成的 rose.petal.0 绑定保留，Contract 只更新因新增字段而后移一行的来源锚点。未声称执行新 CLI 或运行时无障碍审核。
- ADB listener 的既有沙箱权限限制未解除，未重试被拒操作；没有本轮头显拖拽画面、事件坐标、试听、焦点 dump 或 crash buffer。需头显验证 gaze 脱落当帧、静止、每帧拖拽和松手位置，末瓣/暂停/重玩，并对照 DirectPinch 不回归，试听 gain 0.10 与系统背景声并行。主机回归不等于这些设备检查通过。
- 当前快照：`backups/gaze-drag-volume-v2-2026-11-12/`；上一 v1 与更早快照均保留。Git 元数据限制未变，没有 commit。细节见 `docs/gaze-drag-volume-v2-review.json`。

## 音量降低与注视捏合脱离跳位修复（v1，用户反馈未解决）

- 音效音轨准备完成后调用 `AudioTrack.setVolume(0.25f)`，为此前默认线性增益 1 的四分之一。只修改应用音效增益，不调系统音量、不处理 WAV；OBJECT 空间绑定、不申请音频焦点及播完主动停止等上一版逻辑保留。实际响度比例与背景音并播仍需头显试听。
- 注视路径原先每帧把全局设备位置转换为以视图为原点的场景点，然后与抓取时已转换的点相减。精确 6.2.8 转换实现会减去当前 view 中心和 `getLocationInWindow`，因此两个时刻的原点变化也会成为 delta。此源代码缺陷可以产生大幅位移；没有设备事件/画面记录，尚不能确认用户那次跳位中的具体原点变化。
- 现由 PinchDragTracker 保存原始设备位置与原始抓取起点，在同一源空间相减，再由适配器翻转 Y、通过公开 `convertSize` 转换为米制位移，不加入视图平移。来源与起点始终锁定，脱离/元数据种类变化不切源，不使用注视投影命中位置。无效转换记为 Unavailable，恢复继续用原始起点；release 不读取 pose。
- 断开姿态、`PetalDragMotion` 的断开位置减当前 delta 再叠加后续 delta、reparent 顺序、花茎回弹、持有时输入/碰撞保留及松手才飘落都保持原样；不引入位移上限。DirectPinch 仍使用本地 position3D → convertPosition，已有行为不变。4 cm 任意方向阈值、末瓣持有、暂停/取消和重玩流程保留。
- 新增 3 项回归：原点变化不会移动静止持有花瓣，后续三轴长距离移动从断开位置相对叠加；26 个方向的原始像素位移转换后使用同一 4 cm 阈值；无效位移转换后的恢复和无 pose 松手。75 项单测零失败，开发 APK/测试 APK 编译通过；音效 gain 为低风险单值调整，没有为常量编写镜像测试。
- 实验 API 检查通过，无新增 opt-in。SpatialUI 样式 0 errors / 1 条既有用户要求的大字号提示；位置源码检查 0 violations / 2 条已有启发式提示，处置见本轮 JSON。40 个花朵资产、13 个未压缩 GLB、原样 WAV、UI 和无障碍契约与上一快照一致；rose.petal.0 绑定与来源行号未改变。
- 既有 ADB listener 权限限制未解除/重试，没有本轮头显画面、事件轨迹、音频试听或 crash buffer。需真机验证：gazePinch 断开瞬间/静止持有/继续三轴 drag/松手、最后一瓣及暂停恢复；同时对照 directPinch，并试听降音量与背景音并行。模拟转换和位置序列不能证明原生坐标、事件源或显示已经通过。
- 独立快照 `backups/gaze-drag-volume-v1-2026-11-12/` 保留源码、APK 和 SHA-256；上一音频版保留不动。Git 元数据仍受限制，没有提交。详见 `docs/gaze-drag-volume-v1-review.json`。

## 短音效与系统背景音并行播放修复

- 用户反馈每次 petalDrop 播放后，系统背景音消失，下一朵花生成才恢复。代码确认旧 AudioPlayerController 没有自然结束后的停止/收尾路径，started 一直保留到清轮；这与现象一致。没有设备音频焦点日志，未把具体原生焦点标志或混音策略当作已证实根因。
- 改为 Android MODE_STATIC AudioTrack 预载原样 PCM16，通过 SDK 6.2.8 公开的 SpatialAudioTrackExtension 以 OBJECT 模式绑定到瓣根声源。AudioAttributes 使用交互音效的 USAGE_ASSISTANCE_SONIFICATION / CONTENT_TYPE_SONIFICATION；代码不申请音频焦点、不创建媒体会话、不调整系统音量。移除旧 AudioResource/AudioPlayerController 路径，避免保留媒体播放控制器。
- 在最后一帧 marker 回调后主动 stop，丢失回调时由“音效时长 + 250 ms”看门狗停止（当前 WAV 为 325 ms 上限，主线程及时调度前提下）。连续摘取先停止/回绕再播；播放 token 和实际 playback head 双重过滤旧回调。暂停即停止，清轮取消回调、释放音轨与空间绑定，再销毁声源。
- 声源仍在成功 detach、reparent/回弹之前采样连接处，固定在 RoseAnchor 米制坐标，不跟随持有/飘落花瓣。Ready 后独立启动的准备任务与两次真实帧等待保留；异常只记日志，不清花或进入 Error。桥接 OBJECT 的声学参数使用平台默认值，没有声称沿用旧 ObjectAudioComponent 的反平方/零混响配置。
- 新增 11 项主机回归：播完立即 stop、不完整帧不提前停止、重复/旧完成回调与超时、丢失回调、暂停/清轮、原生开始失败收尾、600 次模拟播放，以及原始 WAV 样本、附加 chunk/padding、损坏长度与不支持格式。72 项单测零失败，开发 APK 和测试 APK 编译通过。实验 API 检查通过，无新增 opt-in；SpatialUI 样式 0 errors / 1 条既有大字号提示。
- APK 中 40 个花朵资产与源文件一致、13 个 GLB 不压缩；音效与用户原件/raw 副本字节一致且不压缩，SHA-256 `4b154cdf195b74df0b13fc164365023430d4d18fee50ed0d8dafc23fdbaca361`。场景、输入/持有跟手、花茎、Session、入场、UI 和无障碍契约与上一快照一致。
- 仍需头显验证：背景音乐/系统环境声在首瓣、连续摘取、末瓣长按到松手、结束等待和重玩时始终持续；OBJECT 定位、音量/延迟、暂停恢复及 20 轮资源趋势。既有 ADB listener 沙箱拒绝未重试，没有安装/启动、声音录制、焦点 dump 或新 crash buffer，不能宣称设备问题已实测消失。详见 `docs/petal-audio-mixing-v1-review.json`。
- 当前成果单独归档在 `backups/petal-audio-mixing-v1-2026-11-12/`；Git 元数据权限限制仍未解除，没有声称已提交。

## 脱离后持续跟手、松手才飘落

- 原源码在所有拖拽阶段把位移裁到 12 cm，导致超出后不再跟手；同时在 detach 中立刻移除 Interactable/Collision，这会移除原生输入的必要组件。本次取消位移上限，并把这两个组件的移除延后到 release。Hover 与无障碍 click 仍在 detach 时移除，状态机和 down-target 筛选禁止再次抓取或换手接管。
- 6.2.8 的 `detectSpatialPointerEvent` 为有效 down 保存原 Entity，后续 move/up 使用该缓存。保留原识别器、down 筛选和 Direct 的 view-local 3D / Gaze 的设备位移坐标来源，没有换成屏幕投射位置或增加竞争识别器。具体 ROM 在组件移除时是否中断/缺失坐标没有设备日志，不能将其称为已经实测的原生根因。
- `PetalDragMotion` 保存完整手势位移。在 reparent 后用当前可见 RoseAnchor 姿态减去同一手势位移建立基准；继续移动时只加新位移。跨过阈值的单帧大位移不跳变，往回拖到阈值以内也保持 DetachedHeld，花茎回弹不再带动已摘花瓣。茎干强度仍由原有 strength 上限约束。
- 正常拖拽持有期间不推进飘落或透明度。release 先停止输入/移除碰撞，从当前显示姿态保存 fallStart，再走原有 1.2 秒飘落与淡出。松手不需要空间 pose；暂停、追踪宽限超时和流取消沿用现有取消释放。音效仍仅在 session.detach 成功时一次触发，最后一瓣仍在 release 后才开始祝福流程。
- 新增 4 项位移/基准/清理单测与 1 项双模式末瓣连续序列。覆盖超过 12 cm、XYZ/反向移动、单帧 overshoot、松手快照、重抓清理、末瓣持有时不 Completing、另一手不能接管及无 pose 的 up。真实手部事件、长距离后的空间坐标、release 姿态/淡出和 Native 组件保留效果仍待头显验证，容器裁剪不由此修改。

- 最终开发 APK 与测试 APK 编译通过，61 项单测无失败。实验 API、SpatialUI 样式及位置源码检查通过；已有提示处置见 `docs/held-petal-drag-v1-review.json`。APK 中 40 个花朵资产、13 个未压缩 GLB 和原样未压缩 WAV 校验一致。布局、Session 状态规则、花茎回弹、入场、音频实现与二维 UI 源码保持上一版内容。

## 下移 10 cm 与缩放淡入入场

- RoseAnchor 的位置改为 (0,-0.10,0) m，茎、花冠、花芯和花瓣共用此偏移；底部提示与结果附件保留原坐标。花瓣局部布局、GLB/贴图、数量与花色抽样不改变。
- 6.2.8 公开的 `OpacityControllerComponent` 控制节点及全部后代的透明度。模型装配时比例 1、透明度 0，仍启用以允许骨骼绑定；装配后以 800 ms smoothstep 让整体 scale/opacity 同步从 0 到 1。真实场景帧计时，暂停冻结，清轮/关闭清空。
- 入场保持 Phase.Loading，沿用“花正在盛开…”提示。尚不添加花瓣 Collision/Interactable/Hover 或茎/瓣无障碍动作，普通/直接捏合和无障碍 handler 均拒绝 Loading。ShapeResource 显式全局保留，避免等待期间失效。满比例和透明度 1 提交到场景后，下一活动帧才启用碰撞/交互、进入 Ready、记录花色并启动独立音频任务。
- 保留本轮修改前用户已更新的 `petalDrop.wav` 与 raw 副本（SHA-256 `4b154cdf195b74df0b13fc164365023430d4d18fee50ed0d8dafc23fdbaca361`），没有覆盖或处理音效。声音仍使用下移后的 RoseAnchor 坐标。
- 无障碍 ID、标签与 click 动作保留，在 Ready 时才注册。Contract 源码行号刷新，保留已完成的 `rose.petal.0` 工厂绑定。既有 CLI/设备阻塞仍存在，未宣称重新执行项目/运行时 A11Y 审核。
- `tools/verify_rose_presentation.py` 读取实际 Kotlin 参数及 GLB 顶点。302 组布局 × 5 时刻共 1,510 个包围盒都在 0.60 × 0.70 × 0.40 m 内。完整范围约 X [-9.943,9.911] cm、Y [-30.024,3.222] cm、Z [-7.166,9.495] cm。这是绑定静止网格与均匀缩放的离线证据，原生蒙皮/透明排序、交互开放、暂停恢复和下落效果仍待头显验证。
- 新增 4 项回归：结束前拒绝摘取，缩放/透明度同步且不越界，30/60/90 Hz，清轮/重玩重置，无效帧时间不能提前解锁。

- 最终开发 APK、测试 APK 编译及 56 项单测通过。实验 API、SpatialUI 样式和位置源码检查通过，已有提示在 `docs/rose-entrance-v1-review.json` 处置。APK 中 40 个花朵文件与上一快照一致，13 个 GLB 不压缩；用户更新的 raw WAV 原样且不压缩。外接盘临时断开后已确认源码完整并重新打包，独立快照校验在下方版本记录中保存。

## 音效导致花朵无法生成的回归修复

- 用户反馈上一音效版启动即显示“重新开始”，多次重试仍无花。代码确认音效加载/准备是 `newRound` 的必经步骤，任何音频异常都会进入模型装配 catch 并 `clearRound()`，重试重复同一路径。没有设备堆栈，无法确认最初失败的原生调用或异常文本；不能把路径、线程或多控制器解释称为已证实的设备根因。
- 移除模型资源加载及 Ready 前的音效门槛。模型成功装配后先 `ready/publish/recordReady`，随后独立启动音频任务；`prepareOptionalAudio` 隔离音频异常、保留取消语义。音效失败记录 `PetalAudioPreparationFailed ... flower remains playable`，不清空花朵、不进入 Phase.Error。模型本身的真实装配错误仍保留原错误/重试流程。
- 使用 6.2.8 公开支持的 `AudioResource.load(name, uri, context)`，从 `android.resource://<package>/<R.raw.petal_drop>` 读取原样 WAV。旧 assets 副本移至 `res/raw/petal_drop.wav`，`noCompress += "wav"` 保证原生文件描述符可读。此路径避免本地两篇音频指南对相对路径和 `asset://` 的不同示例。去掉自定义空 mixer 配置。
- 每轮复用一个空间声源和控制器，不再为同一音频资源准备 17–30 个控制器。花朵 Ready 后等待下一实际场景帧，再加载声音；声源挂到 RoseAnchor 后再等一帧才 prepareAudio。保留撕下瞬间的连接点位置、非循环、原生空间声、反平方衰减和暂停停止。准备任务在清轮前取消，原生声音清理异常分别记录，不再阻断花朵清理。
- 新增 3 项音频失败/取消回归和 2 项场景帧等待/取消回归。模拟音频加载失败后仍能摘完 30 瓣并完成；模拟连续 4 轮控制器准备失败仍可摘取；取消仍终止准备。实际启动、原生解码、空间听感与音量仍待头显验证；现有 ADB listener 权限限制未解除。

- 最终开发 APK 和测试 APK 编译通过，52 项单测无失败。实验 API、SpatialUI 样式和位置源码检查通过（样式 1 条已有字体提示；位置 2 条已有启发式提示均在审阅报告处置）。APK 内 raw WAV 不压缩、与用户原文件字节一致；旧 assets 音效副本不再打包；40 个花朵文件、布局/输入/回弹 domain 和无障碍契约均保持上一版内容。详情见 `docs/petal-audio-startup-fix-review.json`。

## 花瓣脱离空间音效（首次接入，已被上方修订替代）

- 根目录用户文件 `petalDrop.wav` 原样打包为 `app/src/main/assets/sounds/petalDrop.wav`，不重采样或改音量。单声道 PCM16、48 kHz、3,600 帧，时长 75 ms。
- 使用 BOM 6.2.8 的公开 `AudioResource`、`ObjectAudioComponent`、`Entity.prepareAudio` 和 `AudioPlayerController.play`。知识库工具当前仅暴露 unavailable 状态，因此按本项目精确版本的公开 SDK 源码核对签名、单声道约束、控制器上限与资源生命周期；没有升级 SDK 或引入实验 API。
- 在 `RoseSession.detach` 成功后立即播放，DirectPinch、GazePinch 与原有无障碍摘取共用该入口。重复回调被会话状态拒绝；未达阈值松手、摘下后继续持有、松手下落都不新增播放。
- 发声位置为受力花冠上该花瓣原接合点，经 `crown.convertPositionTo(spec.position, root)` 换算为 RoseAnchor 米制坐标，在花瓣重挂和花茎回弹之前取样。独立声源位于 RoseAnchor 下，后续不跟随手、下落花瓣或回弹花冠。使用全方向声源、默认 0.1 m 声源半径、反平方距离衰减、音量 1、混响 0；真实响度与距离感未实测。
- 进入 Ready 前为每片花瓣预备一个非循环控制器（最多 30，SDK 公开上限 40 个 object/ambient 控制器）。跨轮只保留一个显式 `toGlobal()` 的音频资源；清轮先停止/关闭控制器并销毁声源，再销毁 RoseAnchor。暂停停止已启动的声音，不在恢复时补播。加载与预备失败走现有可重试装配错误流程，单次播放失败只记录错误并保留摘取结果。
- 头显听感、左右/远近定位、首次摘取延迟、快速连续摘取、暂停/恢复与 20 轮资源趋势待验证；现有 ADB listener 权限限制未解除，本次没有设备试听或原生播放成功证据。

- 最终 `testDebugUnitTest assembleDebugAndroidTest` 与 `assembleDebug` 通过，47 项单测无失败。实验 API 检查通过；SpatialUI 样式检查为 PASS（0 错误，1 条已有大字提示）；位置源码检查为 PASS（0 违规，2 条已有启发式提示，处置见 `docs/petal-spatial-audio-review.json`）。APK 中 WAV 不压缩且与用户原文件逐字节一致，40 个花朵文件与上一完整快照一致，布局/输入/回弹的 domain 源码也保持一致。

## 跨层收拢与舒展

- `interleaved-petals-v6` 为 12 种花瓣分别定义根部半径、高度和杯形开合。外层为 18/24/30/42°，中层为 36/48/54/66°，内层为 60/72/78/82°。外层最收拢的花瓣比中层最舒展的更靠内，中层和内层也有同样的交叠。
- 杯形曲面按各自姿态重新生成，轮廓差异来自不同的实际弧面。保留方位和间距的种子变化，细微旋转、缩放收敛到不会放大陡立曲面误差的范围。根部仍位于实际花托内。
- 内层横向采样从 13 列增加为 15 列，每片 476 三角面，外、中层仍为 412。整体保守上限 19,122 三角面，花茎和贴图继续复用。
- 几何检查覆盖整朵花的所有花瓣对。根部接合区定义为半径 15 mm、局部 Z -10 至 6 mm，区域内的接触保留实际边界记录。区域外相交记为花瓣主体检查失败。报告见 `docs/layout-v6-verification.json`，实际头显显示和动态摘取待验证。

- 47 项单测、开发 APK 与测试 APK 编译、实验 API 和 SpatialUI 样式检查通过（样式保留 1 条用户要求的大字提示）。302 组实际生产布局、82,323 对花瓣未检出接合区以外的主体相交；1535 对接合区接触单列记录。预览见 `art/rose-interleaved-layout-v6-review.png`。APK 内 40 个花朵文件逐字节匹配源文件，13 个 GLB 不压缩；花茎和全部贴图与上一完整快照一致。

## 外层弧面与布局随机性

- `natural-whorls-v5` 把外层也改为包曲膜面，瓣缘轻微回卷。三层局部侧倾均控制在 ±0.4°。原有根部位置继续贴合花托。
- 每圈方位增加 ±20° 变化、开合增加 ±1.5° 变化、大小增加 ±3% 变化。每片花瓣的角位置在平均间隔的 ±9% 内变化，开合增加 ±0.5° 变化，大小增加 ±5% 变化。所有变化在一轮生成时固定，独立种子流不参与瓣数和颜色抽样。
- 47 项单测通过，其中布局随机性回归检查同瓣数配置的间距、开合和尺寸变化。资产检查通过 12 个闭合包曲花瓣、双 UV、纹理和面数。302 组生产布局的 27,624 对同层花瓣未检出相交，根部接合与容积检查通过。报告见 `docs/layout-v5-verification.json`。
- 同为 24 瓣、粉色的种子 0、7、19 的正面与侧面见 `art/rose-natural-layout-v5-review.png`，用于核对轮廓随机变化。额外跨层检查在个别样例的花托边缘检出细小根部接缝，实际观感与动态拖拽仍待设备验证。
- 开发 APK、测试 APK、实验 API 和 SpatialUI 样式检查通过。实际头显显示、舒适性和原生点击仍待验证。

## 中、内层包拢形态

- 用户反馈上一版中、内层的大幅同向侧倾使整朵花显得螺旋展开。`cupped-whorls-v4` 将这两层的侧倾分别从 33/38° 收回到 ±0.2°，径向仰角为 50/74°，根部保持贴合花托外壁。
- 八个中、内层网格的膜面沿花芯周围弯曲，细小的法向深度错位用于相邻边缘叠压。前后表面间距为 0.26 mm，保持闭合和原有三角面预算。外层、花茎与全部贴图的字节保留。
- `verify_rose_assets.py` 从导出字节检查两侧包曲、闭合、UV、面数、蒙皮和贴图。`verify_rose_layout.py` 检查杯形坐标基准与实际生产角度一致，302 组布局的 27,624 对同层花瓣均未检出相交，根部接合与边界检查通过。报告为 `docs/layout-v4-verification.json`。
- 固定预览种子的 17 瓣配置无跨层相交。30 瓣配置检出 6 对跨层接触，对应相交线段的端点均位于闭合花托内部。该补充检查覆盖这两个静止示例。
- 46 项单测、开发 APK、测试 APK、实验 API 与 SpatialUI 样式检查通过。正面、侧面和内层剖视见 `art/rose-cupped-layout-v4-review.png`。头显实际显示与动态摘取待验证。

## 花瓣同层叠压与根部接合

- `seated-spiral-v3` 将外、中、内根部放在花托外壁，半径分别为 7.7、9.5、10.7 mm，花冠局部 Z 为 -8.5、-4.5、-1.8 mm。较上一版下移 8.5、8.5、10.8 mm。根枢轴位于实际闭合花托内部，内层紧接花托上沿。
- 各层花瓣增加 27、33、38° 的同向侧倾，形成相邻边缘的前后叠压。内层根据数量采用 60–70° 的展开角。方位抖动、仰角和尺寸变化收敛到经过检查的范围。运行时与审阅工具都使用 qZ × qX × qY，抓取前姿态与取消回位共享同一变换。
- `tools/verify_rose_layout.py` 从 Kotlin 导出配置和 GLB 读取实际三角面。17–30 瓣、20 个种子的 280 组，加上固定预览和八色配置，共 302 组布局的 27,624 对同层花瓣未检出相交。根部接合与样本容积检查通过，记录见 `docs/layout-v3-verification.json`。检查覆盖静止布局。
- 额外检查固定预览种子的层间相交，在 17 瓣、30 瓣配置中分别检出 8、26 对跨层花瓣相交。层间接触仍需进一步调整。
- 46 项单测、开发 APK、测试 APK、资产检查、实验 API 和 SpatialUI 样式检查通过。离线正面、侧面和内层剖视见 `art/rose-seated-layout-v3-review.png`。头显实际显示和动态拖拽待验证。

## 多轮结束按钮与逐瓣纹理变化

- 源码检查发现，宿主 View 失焦被作为场景暂停处理，附件获得焦点也可能留下暂停状态。现在短暂失焦只取消抓取，Activity 的生命周期控制暂停与恢复。末瓣的真实移除事件直接推进 Completed，按钮淡入随之开始。
- 结束面板在测量祝福前预留 48dp 按钮高度与 24dp 间距，原生 Button 在 Completing 阶段就已挂载。轮次 ID 隔离淡入状态和迟到的重玩回调。祝福保持 600 ms 淡入，按钮在末瓣落完后 450 ms 淡入，满不透明度才可点击。
- 12 个花瓣 GLB 各有两组不同的 UV，共 24 组，保持瓣根 V=1、瓣缘 V=0。每片花瓣使用独立明度值，同层范围为 .86–1、均值 .93。颜色和几何抽样序列保留，变化来自独立的种子流。三张当前花色纹理继续共享。导出网格的顶点、三角面、法线与上一快照逐项一致。
- 46 项单测通过，覆盖连续 36 轮、全部祝福、末瓣身份、迟到移除、逐瓣外观唯一性和每层平均明度。开发 APK、测试 APK、实验 API 与 SpatialUI 样式检查通过。新增 20 轮原生重玩设备测试已编译，当前会话仍受 ADB listener 权限限制，未在头显运行。
- `art/rose-petal-variation-v6-review.png` 用相同生产布局、实际 GLB 和纹理，对比粉、白、橙花瓣变化。当前只确认离线效果，多轮按钮和原生 UV 通道显示待头显验证。

## 轻快回弹、重玩花色与局部次色

- 摘下回弹从 780 ms 缩短为 640 ms，首次反向峰值从初始弯曲的约 25% 降至约 15%。第二次同向峰值约 2%。同一套曲线用于 DirectPinch 和 GazePinch。
- 颜色在本次打开应用内，从排除最近三朵可玩的花之后的候选中均匀抽取。首次启动有 8 种可选，历史只有一朵或两朵时有 7 或 6 种可选，历史满三朵后有 5 种可选。初次花朵计入，失败的加载保留原抽样配置供重试，成功就绪时才写入历史。
- 24 张更新的分层底图均保留主色与由外至内的明度顺序。资产检查还确认白花三层的瓣缘粉色、瓣根暖黄和中部低彩度。八色离线预览使用打包纹理和生产花瓣布局，来源见 `art/textures/generation-v5.md`。
- 44 项单测、开发 APK、测试 APK、资产检查、实验 API 检查和 SpatialUI 样式检查通过。APK 中 40 个 rose 文件与当前源码一致。设备显示、回弹观感和实际重玩仍待头显验证。
- 本次构建沿用工作区已有的图标和语言资源改动。AAPT2 确认此 APK 的默认启动器名称为「一瓣欢喜」，包名仍为 `com.example.flowertears.p18ab52cf`。

## 捏合受力与花茎回弹

- 旧代码在越过 4 cm 阈值的同一回调内先弯曲、再清零。直接跨过阈值的移动更新可能没有可见的弯曲帧。两种捏合继续分别使用视图三维指针和设备位移，转换为场景米制坐标后走同一受力曲线。
- 摘下先保留花瓣姿态并移出花冠层级。花茎从摘下前的弯曲姿态开始做约 780 ms 的衰减摆动，第一次反向摆幅约为原弯曲的 25%，第二次同向摆幅约为 6%。手仍捏住摘下的花瓣时，回弹继续按场景帧推进。未达到阈值就松手时约 220 ms 回位，失焦、追踪超时与轮次清理时复位。
- 41 项单测覆盖了两种捏合的等位移反馈、快速跨阈值时保留弯曲、反向与同向摆幅衰减、重新抓取接续姿态和取消清理。开发 APK、测试 APK、实验 API 与 SpatialUI 样式检查通过。打包的 40 个 rose 文件与上一快照逐字节一致。头显上的注视拖动采样与实际蒙皮回弹待验证，当前会话仍受 ADB listener 权限限制。

## 花茎与花芯细化

- `curved-stem-seated-core-v5` 的下部带轻微弧度，上部前弯从茎高的 64% 处开始加深，较上一版下移 43.2 mm。花冠前伸约 22.8 mm，顶端切线仍保持 35°。三骨骼绑定点、逆绑定矩阵和 Kotlin 前向计算使用同一条静止中心线。
- 花托增加圆弧细分与凹入的花芯底面。花丝和花柱有渐细弯曲，花药与柱头接到各自的支撑端。五片萼片使用有厚度的卷曲曲面。实际 GLB 检查通过茎与花托相接、5 片萼片和 31 根花丝／花柱嵌入花托、31 个顶端接到对应支撑，以及各个曲面闭合。基座 4,842 三角面，30 瓣最多 17,202 三角面。
- `art/rose-stem-core-v5-review.png` 以相同视角对比前后模型，使用导出网格和 Unlit 图集离线渲染。`docs/stem-core-v5-review.json` 的 22 组静止样例均在名义容积内。最终 APK 的 40 个 rose 资源与工作区逐字节一致，较上一快照仅 `stem.glb` 与资产清单有变动。
- 37 项单测、开发 APK 和测试 APK 构建、实验 API 检查通过。SpatialUI 样式检查通过，保留一条既有大字号提示。头显中的显示与动态蒙皮待验证，当前 ADB listener 仍受已有会话权限限制。

## DirectPinch 后拖拽

- 用户反馈 GazePinch → drag 可用，DirectPinch → drag 不工作。原源码虽然允许 InteractionKind.DirectPinch，却对所有模式都要求 inputDevicePose.rawPosition 非零；缺失设备姿态便在开始时拒绝。同时只有 isDownEvent 分支会尝试抓取，首个事件为 Poke/Unknown、后续才报告捏合或有效位置时不会再启动。两者是本次修复的源码路径；没有设备事件日志，不认定为已复现的 ROM 根因。
- DirectPinch 改用公开 SpatialPointerInfo.position3D，以 ViewCoordinateSpace.Local 通过 SpatialViewContent.convertPosition 转入场景米制坐标。零值 local 点可以是有效位置，不再要求 DirectPinch 具有设备 pose。GazePinch/RayBasedPinch/Pointer 保留原 rawPosition → Global → scene 路径，避免改用其投影命中点产生错误三维位移。公开 6.2.8 KDoc 明确 position3D 属于 view，convertPosition 的 Local 来源为当前 View 左上后角、输入像素、输出场景米；没有手动猜测缩放或重复翻转 Y。远端 rawPosition 原有物理单位/目标 ROM 校准缺口保留。
- 新 PinchDragTracker 只在真实 down 且命中可摘花瓣时登记候选；Poke/Unknown 可等待后续实际 Direct/Remote pinch 与首个有效坐标，但不会把普通戳碰或移动本身作为摘取。开始后固定原花瓣、pointerId、坐标来源和原点；重复 down 不重新归零、后续 kind 缺失/变化不切换来源，也不跟随当前命中目标跳到其它花瓣。
- 已抓取后无需再次命中原碰撞体；达到 4 cm 仍由 RoseSession 幂等结算一次，摘下移除组件后继续持有跟手，up 无需空间 metadata/pose 即可触发飘落。无效移动坐标进入现有 250 ms 宽限；焦点暂停、流取消、超时释放与新轮清理均清空捕获，迟到移动不会复活旧抓取。第二只手已经按下时不能在首只手松开后自动接管，需新一次 pinch。仅一个 detectSpatialPointerEvent 处理所有输入，没有新增会竞争消费的第二个拖拽识别器。
- 新增 8 个 PinchDragTrackerTest，与真实 RoseSession 逻辑组合运行，覆盖：无设备 pose 的直接捏合与摘下后持有/松手；Gaze 不使用投影指针位移；Poke/Unknown 延迟变成 Direct 且固定初始花瓣；无效 Direct 坐标不退回设备 pose 并可恢复；第二只手/预先按下候选不能偷接管；拒绝、取消、清理后的新 down 要求；26 个正负轴向/斜向（包含纯 Z）4 cm 阈值与重复 down 不归零/不重复计数。测试输入已是米制的合成坐标，没有在 Android PointerInput 或 ROM 手追踪上注入动作。
- 首次教学与附件可访问状态改为“看向或靠近一片花瓣，捏住，轻轻拉开。”；LaunchSmokeTest 对应更新，37/37 单测、assembleDebug、assembleDebugAndroidTest、实验 API 检查通过。SpatialUI 样式 0 errors / 1 既有大字号 warning；无障碍源码审计 0 errors / 0 warnings，保留 rose.petal.0 手动完成的动态工厂绑定。placement gate 0 violations / 2 既有 advisories；仍无 SDK visual bounds，surface-detail 仍是摘取标签的启发式，未改动空间布局。
- SDK 依据为缓存的精确 6.2.8 公共源码/KDoc（SpatialPointerInfo、InputDevicePose、SpatialCoordinateSpaceConverter），新调用无实验标记。已有知识图谱不可用；官网检索未提供可替代精确版本的事件语义，未据其推断 6.2.8 API。模型、24 张贴图、抽样、UI 样式和体积参数与 v4 快照保持一致。仅更新手势路由和教学文字；最终 APK rose 资源集合及字节与 v4 相同。
- 仍受既有 ADB listener Operation not permitted 限制，没有再次尝试已拒绝操作。DirectPinch 实际 down/kind 时序、视图三维指针连续性、拖离 collider 后回调、直接/注视模式临界切换、眼手坐标与 4 cm 实距均需头显验证。新增 PetalGrab 日志记录 kind/source/pointer/session/petal 便于获取设备证据，不记录坐标。版本源码与 APK 单独保存，旧 v4 快照保留；不声称原生 DirectPinch 运行验收或 Git commit 已完成。

## 轻微承重花茎、蓝色妖姬与分层底图（上一版本）

- 用户澄清替换的是奶油黄色组：colorId 4 从 cream 改为 blue-enchantress，colorId 3 白色保留；红、粉、浅粉、黄、橙、淡紫与八路独立均匀抽样继续保留。没有改动花瓣数量、结果状态机或随机抽样顺序。
- `weighted-neck-v4` 取消上一版 S 形：下部 82% 是约 1.909° 倾斜的直线，顶部最后 43.2 mm 才向花冠方向平滑前弯，前伸量约 10.083 mm。曲线横向单调，不左右摆动，正侧面已检查 `art/rose-neck-v4-review.png`。花托、顶端共同生长轴和三骨骼数量保留。实际 GLB 检查主体偏离直线不到 1 µm（数值量级），终端截面/花芯/花瓣生长轴仍一致。
- 绑定根/中点随主体平移倾斜，逆绑定矩阵同步重建，tip 仍在原花冠原点。`domain/RoseStem.kt` 显式声明作者绑定坐标；生产 Kotlin 导出的三点与实际 GLB 逐点比对，误差小于 0.1 µm。花冠前向计算改用这些作者坐标与既有旋转约定，不把坐标空间说明不足的 SDK pose.position 当作 parent-local。原生 pose 旋转/动态蒙皮的既有真机缺口保留，不将离线绑定检查看作设备验证。
- `layered-basecolor-v4` 由内置 ImageGen 制作两张 4×3 图集，导出 8 色 × 外/中/内共 24 张独立 RGB 底图。每一张均有独立的细脉络/表皮变化；每个颜色的平均线性亮度满足外 > 中 > 内。初稿映射后色差过强，最终版通过 ImageGen 编辑收敛色差，再次检查整花；相邻层平均明度差约 2.36–9.50 L*，没有用材质纯色倍乘来代替层纹理。最终源图和完整生成/修订提示词在 `art/textures/generation-v4.md` 及其链接的 refinement 文档；导出仅裁切、缩放和格式转换。
- `RoseLayout.texturePath` 统一决定运行时与审阅模型的纹理路径；单测导出每瓣真实路径，资产校验确认所有 17–30 瓣样本及八色均绑定三张正确底图。旧的八张单色整花 PNG 从当前资源目录移除，旧版快照保留。12 个花瓣 GLB、中性原型底图和茎叶图集仍与上一版一致。材质仍是白色因子的 Unlit，sRGB、mipmap 与 clamp-to-edge 规则保留。
- 为避免从 8 张缓存扩大到 24 张，`RoundAssetSet` 只固定当前花色的三层资源，清理旧轮全部 Entity/Material 后才换组释放；相同花色重玩复用，部分失败可续载/重试，退出释放。新增 3 项模拟资源回归：20 轮切色/同色复用后只保留三张、加载中途失败后保留有效同组并重试、单层失效重载不重建其它层及幂等关闭。此为模拟 SDK 所有权的测试，不是实际 GPU 内存或 20 轮头显性能测量。
- 最终 29/29 单测、assembleDebug、assembleDebugAndroidTest、实验 API 检查通过。资产检查通过闭合/预算、倾斜直杆与短颈、三关节/权重/逆绑定、24 个不同哈希与细节模式、白色保留、蓝色三层、全部颜色明度顺序和生产纹理路径。基座仍 2,084 面、30 瓣最多 14,444 面。UI 样式 0 errors / 1 既有大字号 warning；无障碍源码审计 0 errors / 0 warnings，保留 `rose.petal.0` 工厂手动完成声明。placement gate 0 violations / 2 既有 advisories，仍缺 SDK visual bounds；surface-detail 提示来自标签启发式，本次没有为单张视图推近实体。
- 当前离线图为 `art/rose-layered-v4-eight-colors.png`、`art/rose-layered-v4-blue-detail.png` 和 `art/rose-neck-v4-review.png`，直接采样实际网格/UV/颜色，未加入诊断灯光。固定种子的 17–30 瓣静止几何仍位于名义容积内；不证明所有种子无穿插或动态边界通过。旧 v2/v3 图片继续作为历史保留。
- 最终 APK 内 rose 文件集合与源目录精确匹配，包含 13 个 GLB、24 张分层底图、中性底图、茎叶图集和 manifest，无旧单花色 PNG 残留。设备 ADB listener 权限阻塞未重试，没有头显安装/观测结果；新花色、层间自然过渡、受力/复位、首次三图加载及连续重玩仍需设备验收。本次源码与 APK 存入独立 v4 快照，Git 限制未绕过。

## 自然花茎与统一花冠朝向（上一版本）

- 用户要求花茎自然弯曲、顶部花芯与花瓣协调朝上并略偏 camera。`organic-stem-v3` 改动实际 GLB 的静止网格：33 圈截面垂直于曲线切线，形成约 24.6 mm 横向跨度的柔和 S 形，渐细并在生长节附近轻微膨大。刺随曲面重新定位，花托由两圈直边锥筒改为五圈圆润过渡。顶部截面切线沿 `(0,0.819152,0.573576)`，与花冠生长轴一致。
- 发现花芯原在 stem_tip 节点下，而花瓣由独立 CrownMotion 的手工前向运动学驱动；两者只有静止欧拉角的数值对应。本次在骨骼就绪后，把三处花芯/萼片/花丝网格明确迁入同一个 CrownMotion，局部旋转 +90° 将 core 的 +Y 对齐 blossom 的 +Z。CrownMotion -55° 作用于全部花冠，静止整体高于水平 55°、偏向前方 +Z。不再依赖原生骨骼节点是否同步移动普通 Entity 子节点。没有设备日志或动态捕获，因此不将原结构认定为已在设备上复现的根因。
- 独立 GLB 的 core 旋转同步改为 +35°（原源文件为 +70°，运行时另行覆盖）；在直接预览 GLB 和在 app 中共同挂载时，静止朝向均协调。源网格基座变更有新资产库版本 `rose-llm-v2`；12 个花瓣 GLB、全部中性/八色/茎叶底图逐字节对照上一完整快照保持一致，`layered-bowl-v2` 的抽样和花瓣布局保留。
- 保留原三骨骼及逆绑定矩阵，曲线在 y=-0.24/-0.12/0 三个绑定点上分别经过原骨骼中心，原受力前向计算仍适用。静止曲线烘焙在网格里，归零骨骼恢复这条自然曲线；不是仅靠开场姿态把直杆临时掰弯。原骨骼 pose 公共 KDoc 对坐标空间说明不足的限制仍存在，本次未新增相关 API，也不以离线测试认定原生动态蒙皮语义已验证。
- `verify_rose_assets.py` 复读最终字节，通过闭合花瓣、UV 焊接后的茎/刺闭合、权重归一化、三绑定点与曲线重合、实际顶部截面法线/源 core 四元数/生产 Kotlin 导出的花冠轴一致、图集与八色检查。基座现 2,084 面，30 瓣最多 14,444 面，均在原预算内。
- `art/rose-stem-v3-review.png` 使用实际旧完整快照与新 GLB，对照直茎/曲茎、正/侧面、17/30 瓣和花芯生长轴。使用同一离线 Unlit 采样；已检查可见曲线与朝向。`docs/stem-v3-review.json` 中固定种子的 17–30 瓣静止网格仍位于名义 0.60 × 0.70 × 0.40 m 容积内；没有 SDK 实测 visual bounds，不证明所有种子无穿插、动态拖拽不裁切或运行时显示已通过。
- 最终 26/26 单测、assembleDebug、assembleDebugAndroidTest 和实验 API 检查通过。SpatialUI 样式 0 errors / 1 既有大字号 warning；无障碍源码审计更新 source 锚点并保留 `rose.petal.0` 动态工厂完成绑定。placement source gate 0 violations / 2 已审阅 advisories：仍缺 SDK visual bounds，surface-detail 提示由摘取标签启发式触发；本次实体层次服务于同一花冠方向，没有为单张正面截图额外推近花。
- 未重复尝试已拒绝的 ADB listener。头显仍需确认花芯与花瓣受力时同步、末瓣摘下后的花芯朝向、追踪取消/暂停后的曲线复位、连续重玩和真实遮挡。新源码与 APK 独立存档，上一材质版本完整快照保留，Git 元数据权限限制没有绕过。

## 基础贴图自然化（上一版本）

- 原花瓣使用接近均匀的 256px 灰白底图乘纯色，茎叶花蕊只有纯色因子。现采用 `botanical-basecolor-v2`：内置 ImageGen 制作鲜嫩花瓣表皮、细脉络、根部到边缘的色素过渡，以及绿色花茎纹理、萼片叶脉、花粉颗粒和花丝细纹。完整提示词和源图见 `art/textures/generation.md`。图片生成与网格建模来源分别记录，没有把已有参数化网格描述为云端生成。
- 最终花瓣提供 8 张独立 512 × 512 RGB 底图，保留乳白根部与彩色边缘；运行时使用白色因子，避免再乘纯色破坏渐变或污染白/黄花。中性原型底图仍保留。每个 colorId 按需加载，沿用 `RetainedAsset` 的 `valid` / `toGlobal()` / 场景关闭时 `close()`，最多保留八张；不在每轮或每片上重建图片。
- 花瓣 UV 根部 V=1、边缘 V=0，正背面一致。茎和管状花蕊 UV 现在沿长度展开，重复接缝位置避免跨整张图片插值；萼片映射到独立叶脉区域。基座共享 1024 × 1024 图集，各区 UV 内缩 4px。各部件不再次乘绿色/黄色；花粉仅用中性 0.88 亮度因子，为原有结束微亮保留余量。
- 根据精确 SDK 6.2.8 公共 KDoc，显式使用 TextureCreateOption 的 sRGB 编码/色域与生成 mipmap 默认值；花瓣用 MaterialTextureParameter、双轴 CLAMP_TO_EDGE 和默认三线性采样，避免根部/外缘绕回取样。新接口无实验注解，实验 API 检查通过；未依赖私有实现。知识图谱缺失的既有环境事实保留。
- 对照 `backups/baseline-2026-11-10/` 内实际旧源码归档：13 个 GLB 的每个三角形位置字节一致，三关节变换、逆绑定矩阵和每个位置对应的蒙皮权重一致。单片仍 412 面、基座 1,692 面、30 瓣最多 14,052 面。UV、接缝顶点索引和材质/贴图发生变化，不将整个 GLB 字节说成未变。详见 `docs/materials-v2-review.json`。
- `verify_rose_assets.py` 通过：闭合花瓣、预算、三骨骼/权重、UV 范围/方向、图集三角形不跨色区、嵌入/独立贴图字节一致、三张源图哈希、八个不同花色导出及白/黄无粉色污染。最终 APK 内 24 个 rose 文件逐字节匹配工作区，包括 13 个 GLB 和 8 张完整花色底图。
- 离线图 `art/rose-materials-v2-comparison.png` 使用同一模型/布局/相机对照前后效果；`art/rose-materials-v2-eight-colors.png` 展示八色，`art/rose-materials-v2-detail.png` 展示局部。工具直接采样实际 GLB 的 UV/嵌入 PNG，按 sRGB 解码并计算 Unlit 颜色，无诊断灯光；不模拟设备色调映射、实际 mipmap/显示、房间对比度或透明排序，不当作运行截图。旧 Editor Sources 与旧诊断图保留历史用途。
- 最终 `assembleDebug`、26/26 单测、`assembleDebugAndroidTest`、实验 API 检查通过。SpatialUI 样式仍 0 errors / 1 已审阅的大字号 warning；无障碍源码审计 0 errors / 0 warnings，更新源码锚点并保留手动完成的 `rose.petal.0` 工厂声明。placement gate 0 violations / 2 既有 advisories：未取得 SDK visual bounds；“摘取花瓣”标签触发启发式 surface-detail 提示，几何/布局未改。没有重复尝试被拒绝的 ADB 监听操作。
- 本次没有头显安装或观测证据。仍需设备核对新贴图方向、八色显示与不同距离的过滤效果，以及连续重玩时资源保留。包名、花色/数量抽样、花瓣布局、交互和全部 UI 语义/动画保持原流程。

## 版本基线准备

用户已授权建立 Git 基线与后续独立提交。当前会话执行 `git init -b main` 被 `.git` 路径的文件权限拒绝（Operation not permitted），没有仓库、commit 或 tag 创建成功；未通过其它 Git 目录或上下文绕过限制。已完善 `.gitignore`、AGENTS 中的后续提交约定和 `docs/version-control.md` 的本机终端初始化/回滚步骤，并在 `backups/baseline-2026-11-10/` 保存当前完整源码归档、独立 APK 与逐文件 SHA-256 清单。该快照不补造此前未保存的完整历史，也不将当前构建认定为头显验收通过。

## 结束淡入（上一补充）

- 祝福在进入 Completing 时通过 Compose Animatable 从 alpha 0 淡入至 1，时长 600 ms；Completing → Completed 不重新播放或换句。按钮仍在末瓣飘落后的 Completed 才出现，并独立以 450 ms 淡入。白色、80/104sp 共用文字样式、整组居中和 48dp 按钮占位保留。
- 淡入期间的按钮不执行重玩回调，且原生子 semantics 暂不公开；完全显示后恢复正常按钮节点与动作。保持原生 Button enabled 视觉，避免禁用态 alpha 与淡入相乘导致末尾突然变亮。Error 的“重新开始”保持立即可用，新轮将两段动画复位。面板 owner 的结束 value 只描述本轮祝福，不提前声称按钮可用。
- 本次为 Compose 透明度与交互呈现修改，执行 assembleDebug、实验 API、SpatialUI 样式检查及无障碍源码审计，不新增或重跑行为单测。原有 26 项通过记录保留；样式仅有已审阅的大字号提示。当前 ADB 权限限制下没有头显动画录屏或淡入点击验证。
- 继续使用 SpatialUI Button / Text 与根 PicoTheme；原生色表、按钮颜色/hover、透明 DefaultWindowContainer、隐藏底板及无边框/无新增材质均保留。动画只作用于结束内容透明度，不实现自定义 hover。

## 首启二维附件与随机居中祝福

- 用户补充确认首次启动模型可见、所有文字不可见。此前首帧修复只约束了模型加载，AttachmentPanel 仍在首帧前声明，而且 owner 只在 initial 查找一次。精确 6.2.8 SDK 的 legacy delegate 对原生显示 owner 构造异常会缓存 empty owner；这与冷启动文字缺失吻合，但没有设备日志，不能断言已复现该 SDK 分支。
- 现在 `attachmentsReady` 只在首个实际 SceneEvents.Update 后置位，随后才声明两个稳定 ID 的 AttachmentPanel。initial 立即返回；`bindPanels` 集中解析公开的 SpatialViewAttachments，迟到的 owner 通过 update 与按帧等待补绑；AttachmentBinding 保证同一 owner 仅配置一次，被宿主替换的有效 owner 可以重新绑定。初轮模型装配在两个 UI owner 成功绑定后开始。没有修改 SDK/Host 或依赖其内部显示 owner 接口。
- 完成阶段的 message owner 从底部移至视图坐标 `(0,0,0.12)m`；祝福和原生重玩 Button 置于同一横向居中的 Column，整组围绕窗口中心排列。结果与祝福共用 RoseLargeText（headlineMedium 原字号/行高乘 4，SDK 默认 80/104sp、白色、居中），祝福允许换行。旧固定 128dp 高度改为最小高度，内容可自然增高；最后一瓣飘落时预留 48dp 按钮位置，按钮仍到 Completed 才可点击。
- RoseBlessings 收录 12 条短祝福，例如「愿你心有暖阳，笑意常在。」「愿你所遇，皆有小欢喜。」「愿你与美好，不期而遇。」仅在最后一瓣释放时随机选取一次并保存在 RoseSession；Completing、Completed、暂停/恢复与重绘不重抽，新轮清空旧句。抽样不改变花瓣数/花色、摘取状态或结果公式；无障碍 value 使用实际选中的句子。
- 新增 7 个单测：4 个附件晚到/重复绑定/宿主替换/失败重试与双 owner 就绪检查；3 个祝福的末瓣释放单次抽样、重玩清空与独立抽取、短句/唯一性/可达性/可复现性检查。现共 26/26 通过，原有计数、随机、方向和生命周期逻辑回归继续通过。
- LaunchSmokeTest 从只检查 Activity 存活升级为同一次 Activity 启动中检查两条初始教学文字同时可见。使用公开 UiAutomation/AccessibilityNodeInfo 读取所有显示器的原生 UI 节点，只在测试连接中临时打开窗口检索 flag 并恢复，不安装或启用 AccessibilityService，不点击、不读取私有 ECS。仪器测试 APK 编译通过；ADB 阻塞下没有执行该设备断言，不能以编译代替首启显示验证。
- `assembleDebug`、`testDebugUnitTest`、`assembleDebugAndroidTest`、实验 API 检查通过。样式 0 errors / 1 已审阅的大字号 warning，无障碍源码审计 0 errors / 0 warnings；Contract 的两附件 source 迁至 RoseScene.bindPanels，保留 rose.result / rose.message 及 rose.petal.0 工厂完成绑定。placement gate 0 violations / 2 既有 advisories，未取得实际 SDK visual bounds。
- 仍需头显验收：全新进程首屏 UI 可见、提示与模型同时就绪；每条大字祝福无裁切、祝福/按钮居中；重玩清空旧句且不重复绑定；眼手输入不被透明 UI 命中区遮挡。当前构建与逻辑证据不证明这些显示、输入和舒适性检查已通过。

UI 样式：继续使用 `com.pico.spatial.ui.design.Text` / `Button`，根 Main.kt 的 PicoTheme、原生 ColorScheme 和 Button 背景/hover 保留；roseTextWhite 是结果与提示唯一自定义 UI 色，Button 通过 colors API 提供同色前景，slot 不重复覆盖。唯一 DefaultWindowContainer 的 manifest materialbackground=0 和隐藏底板保留，结构透明，无新增背景、边框或材质。白字真实环境对比度未观察。

## 应用中英文名

- 中文显示名按用户要求为「一瓣欢喜」，资源位于 `values-zh/strings.xml` 和 `values-b+zh+Hant/strings.xml`，包括港、台中文环境也保留用户指定的完整名称；默认/英文显示名为 `RoseTear`，位于 `values/strings.xml`。manifest 继续使用 `@string/app_name`，locale-config 在原有 en-US 上增加 zh。包名、代码类名与模型路径保留。
- `assembleDebug`、实验 API 与 SpatialUI 样式检查通过。已用 Android Build Tools 35 的 AAPT2 直接检查生成 APK：默认和英文标签为 `RoseTear`，所有输出的中文标签为「一瓣欢喜」，资源表存在 `(zh)` 对应字符串，详见 `docs/app-name-verification.json`。这次仅改名称资源，未增加或重跑行为单测；未执行头显启动器语言切换。

## 底部白字与图标打包

- 用户要求底部全部提示为白色：Loading、Error、追踪不可用、Completing/Completed 祝福与两条首次教学文字均改用 `roseTextWhite=#FFFFFF`。结果文字继续共用白色且保持 80/104sp。原生重玩 Button 通过 `ButtonDefaults.buttonColors(contentColor=roseTextWhite)` 统一前景，内部 Text 继续继承组件颜色，默认背景和 hover 保留。原生 PicoTheme/ColorScheme 未替换，窗口仍透明，无新增面板、背景、边框或材质。
- 图标请求的原图为工程根目录 `roseTear.png`。内置 ImageGen 生成了 `art/app-icon/rose-tear-foreground.png`，已检查透明背景合成效果，生成提示词和保存记录在 `art/app-icon/generation.md`。
- 工作恢复时工程已有更新的 PICO 立体图标和 manifest 配置：icon/roundIcon 为 `@mipmap/ic_spatial_launcher`，`icon.3d.list` / `icon.sdf.list` 引用各两层 PNG。已查看其原图花朵与心形花瓣效果并保留这套当前配置；生成的紧凑图是备选，没有覆盖这些现有资源，也没有改动现有应用名/locale 配置。
- 本次为颜色与资源打包修改，执行 `assembleDebug`、实验 API 检查、SpatialUI 样式 verifier 和无障碍源码审计。样式 0 errors / 1 warning，唯一 warning 为既有结果大字号；未新增或重跑行为单测，19 项通过记录来自前次启动修复。设备连接权限限制保留，白字实际对比度与 PICO 启动器立体图标显示未在头显验证。

## 冷启动修复

用户反馈：第一次启动没有花，需要关闭后再次打开。本次没有取得设备日志，也未确定当时 UI 停在 Loading、Error 还是模型不可见；以下为查明的源码缺陷和修复，不认定为真机根因复现。

- 原 `remember { RoseScene() }` 的销毁放在 `DisposableEffect(scene,lifecycle,view)` 中。宿主 lifecycle/view 变化会执行旧 effect 的清理并永久置 `disposed=true`，但 remembered scene 本身没有随这两个键重建，随后会复用已关闭的场景。这是与首次窗口绑定时空白现象吻合的生命周期缺陷。现在单独的 `DisposableEffect(scene)` 管理真正的场景退出，监听重绑只卸载旧 observer/listener。
- `SpatialView.initial` 不再等待完整加载和骨骼准备，完成附件/订阅注册并启动受场景生命周期约束的任务后就返回。初始化任务等待本 SpatialView 的实际 `SceneEvents.Update` 后才开始首轮加载，并记录首帧日志，避免早于原生空间视图出帧就访问导入资源。
- `SceneFrameGate` 保留已到达的帧信号；骨骼等待改为按实际场景帧重读数据，最多 600 帧，替代原先 60 × 16 ms 的约一秒等待。尚无帧时不重复查询骨骼或消费帧预算。真正缺失的骨骼仍会失败，保留错误与重试，不隐藏故障或改变随机参数。
- 关闭场景时显式取消初始化/装配 Job，再取消订阅和释放资源；`close()` 幂等。已取消的等待不会在后来到达的帧上继续建花。初始化入口防止重复注册/重复启动。
- 新增 6 个 `SceneFrameGateTest`：首帧前不加载；超过旧 60 次限制的慢骨骼仍可就绪；无帧不消费预算/不轮询原生状态；等待前的帧不丢失；真实缺失到达帧预算仍失败；退出取消后不会因后续帧加载。此为可控帧信号的单测，未在 Android 宿主中动态重绑 lifecycle，也不代表设备冷启动验证。
- 当前 19/19 单测、`assembleDebug`、`assembleDebugAndroidTest`、实验 API 检查均通过。SpatialUI 样式仍 0 errors / 1 warning（既有用户要求的大字号）；Spatial 无障碍源码审计 0 errors / 0 warnings，保留已完成的 `rose.petal.0` 工厂声明。placement gate 仍为 0 violations / 2 已审阅 advisories。
- 原生事件与初始化顺序依据本地精确 6.2.8 public SDK 的 `SpatialView`、`SpatialViewContent.subscribe`、SceneEvents KDoc 和既有项目模式；未使用 SDK 内部 rootEntity 或生命周期回调接口。知识图谱和设备连接的已有阻塞未改变，未重试已拒绝的 ADB 监听操作。
- 仍需设备验收：全新进程首次打开即显示花、慢加载后自行就绪、临时失焦/宿主重绑不清空场景、加载中退出后不复活旧花，以及重玩继续可用。构建、单测和源码审阅不能证明上述头显行为已通过。

本次只调整生命周期与加载顺序。SpatialUI 仍使用 `Button` / `Text` 与根 `PicoTheme`，白字 token `roseResultWhite=#FFFFFF` 保留且不替换原生色表；唯一 DefaultWindowContainer 仍以 manifest `materialbackground=0` 关闭玻璃并隐藏底板，结构透明，无新增背景/边框/材质。按钮与花瓣继续使用 SDK hover，按钮 slot 继承原生颜色；真实环境白字对比度仍待观察。

## 第二次交互修订（布局与重玩）

用户反馈：花瓣像平面贴片朝向相机；重玩会进入“花没有准备好”；结果文字需在上一版基础上再放大一倍并改白。

- 布局改为 `layered-bowl-v2`：三层根部半径 12/9/6 mm，根部沿花冠轴抬高 0/4/9 mm，花瓣局部倾角分别约 25/49/73°，具有种子控制的小幅变化与层间错位。每片先在自己的径向平面内倾斜，再绕花冠轴旋转；受力与取消也使用同一四元数顺序。
- 花冠 X 轴旋转由 -20° 调整到 -55°，生长轴约为 `(0, 0.819, 0.574)`，自然朝上且偏向观察者。已有花芯与萼片节点的 X 旋转在运行时从源文件的 +70° 覆盖为 +35°，使花芯和花瓣的轴一致，骨骼短茎保持直立。12 个独立花瓣与三骨骼 GLB 的几何字节均未修改；manifest 保留原始建模来源并记录运行时布局。
- 重玩路径中发现公用 `neutral.png` 仅凭 Kotlin 非空引用跨轮复用。SDK 6.2.8 公共 Resource KDoc 与技能的 resources-loading.md 均说明：资源可随拥有它的 Entity/Material 销毁而失效，跨轮使用需 `toGlobal()`，结束时主动 `close()`。新 `RetainedAsset` 在保留前检查有效性，显式固定生命周期，失效时重载，场景关闭时释放一次。原型 Entity 同时增加 `valid` 检查。
- 本次没有设备异常堆栈，因此纹理失效是与反馈吻合的源码缺陷，不能据此断言已经覆盖设备上所有失败原因。另为新导入的实际 SkeletonPoseComponent 增加最多约 960 ms 的就绪等待，避免附件当帧尚无骨骼数据就报错；仍严格要求三骨骼和关节名，不使用替代花。错误日志包含轮次、seed、装配阶段和堆栈，便于定位后续设备失败。
- 结果字体在上一版 40/52 sp 上再翻倍，使用 `headlineMedium` 原值乘 4，SDK 6.2.8 下为 80/104 sp。颜色为用户指定的 `roseResultWhite=#FFFFFF`，原生 SpatialUI ColorScheme 不改。面板高度随文字增长，保留系统字体缩放。
- 单测 13/13：原 7 个状态机/随机/距离测试；2 个布局测试（280 个数量/种子组合的层次、完整环绕、内层向上，以及导出真实生产布局供预览）；4 个资源所有权测试（20 轮模拟清理后仍复用同一资源、失效重载、加载失败重试、固定生命周期失败后清理与重试）。资源回归使用依据 SDK 所有权规则编写的模拟资源，不代表头显连续重玩或性能验证。
- `tools/create_review_assemblies.py` 不再复制布局算法，而读取 `RoseLayoutTest` 导出的实际 Kotlin 数据，复读现有 GLB 字节生成审阅快照。`tools/render_rose_review.py` 对实际快照网格做离线正/侧/俯视光栅化。已检查 `art/rose-layout-v2-threeviews.png` 的 17/30 瓣侧面杯状层次与俯视环绕关系。使用诊断光照，不证明运行时 Unlit 材质或骨骼效果。
- `docs/layout-v2-review.json` 记录一个固定种子的全部 17–30 瓣静止网格包围盒，均位于名义 0.60 × 0.70 × 0.40 m 容积内。此为离线几何证据，未取得 SDK 实际 visual bounds，不证明所有随机布局无穿插、可达性或动态拖拽不裁切。
- placement source gate 通过，0 violations / 2 advisories。`RoseAnchor` 为既有命名 owner，但没有运行时边界证据，故保留 containment 提示；surface-detail 提示来自“摘取花瓣”标签启发式，实际 Z 用于三层生长轴与根部高度，没有为正面可见性把整朵花推向相机。两项均不认定为运行时验收通过。
- `assembleDebug`、`testDebugUnitTest`、`assembleDebugAndroidTest`、实验 API 检查和 GLB 字节级资产检查通过；SpatialUI 样式 0 errors / 1 warning（用户明确要求的大字号）；Spatial 无障碍源码审计 0 errors / 0 warnings。保留 `rose.petal.0` 工厂完成声明，实际节点仍逐片注册 `rose.petal.<id>`。
- 设备操作沿用 ADB listener 权限阻塞，未再次尝试已拒绝的监听操作。头显安装、重玩恢复、80sp 白字的实际裁切与对比度、手部输入、骨骼姿态和淡出仍待验收。旧 Editor Sources 未同步，本次不声称 Editor 实时验证。

UI 样式交付复核：使用 `com.pico.spatial.ui.design.Button`、`Text`，根 `Main.kt` 的 `PicoTheme` 保留。自定义 UI 色仅 `FlowerVolume.kt` 中标注用户来源的 `roseResultWhite=Color(0xFFFFFFFF)`，只用于结果文字；其它提示使用原主题语义色，按钮内容继承原生状态前景。唯一 DefaultWindowContainer 仍以 manifest `materialbackground=0` 关闭玻璃，并隐藏底板；根与结构布局透明，无新增背景、边框或材质。按钮使用原生 hover，花瓣使用 HoverEffectComponent，结果透明度动画只用于文字切换。白字在真实环境中的实际对比度尚未观察。

## 第一次交互修订（历史）

- 用户追加要求优先于 PRD 原方向限制：以捏住时为原点，使用三维欧氏距离，达到原有 4 cm 阈值即可摘下；向内、向后与斜向均有效。8 mm 受力死区与单次结算规则保留。单测覆盖 26 个轴向/斜向方向的阈值内外以及三轴合成距离。
- 「喜欢／不喜欢」使用原 `headlineMedium` 字号与行高的两倍，结果面板以 96 dp 为最小高度、允许随字体增长，单行显示。本地 6.2.8 资源的原始字号/行高为 20/26 sp，放大后为 40/52 sp；保留系统字体缩放。例外依据为用户 2026-11-10 的明确字号要求。
- 提示和重玩按钮合并为 `rose_message` 内的原生 Column 上下布局，移除同深度独立 `rose_replay` 面板；排除透明面板间的命中遮挡。花瓣手势仅绑定仍可摘取的有效网格，结束后不再遍历已销毁花瓣来匹配按钮输入。
- 按钮通过受状态保护的 `replay()` 入口启动，协程立即执行至首次挂起，在异步加载前锁定入口并清空结果；新轮重置手部位移和持有姿态。UI 状态在附件内容中读取，结果变化不再驱动整个 SpatialView 重建附件声明。状态发布与当前 UI 比较，消除错误重试回到相同 Ready 状态时可能停留 Loading 的问题。
- 本次 `assembleDebug`、`testDebugUnitTest`（7/7）、`assembleDebugAndroidTest`、实验 API 检查通过。更新后的 Spatial 无障碍源码审计为 0 errors / 0 warnings；Contract 已刷新为两个附件 owner，并按实际工厂源码重新完成 `rose.petal.0` 声明。
- 本次未进行设备安装、启动或按钮点击验证，沿用 ADB listener 的权限阻塞；上述修复为源码和构建证据，不能视为头显交互验收。

UI 样式复核：继续使用 `com.pico.spatial.ui.design.Button`、`Text`，根 `Main.kt` 的 `PicoTheme` 包装保留；没有新增自定义 UI 颜色，文字使用 `labelPrimary` / `labelSecondary`，按钮内容继承原生状态颜色。唯一 DefaultWindowContainer 仍在 manifest 中关闭 materialbackground（0）并隐藏底板，布局透明，无新增背景、边框或材质。按钮使用内置 hover，花瓣保留 SDK HoverEffectComponent；结果透明度动画仅用于文字切换。样式 verifier 为 0 errors / 1 warning，大字号提示依照本次明确要求保留。实际对比度、裁切与可点击范围仍需头显验收。

## 已有证据

- Gradle `assembleDebug` 成功，生成 arm64 debug APK。
- `testDebugUnitTest`：当前 26 个测试。原 7 个遍历 17–30 的奇偶序列及多种摘取顺序，覆盖取消、双手互斥、重复结算、旧会话、末瓣持有/释放、暂停、非法配置、3,000 个种子覆盖全部 112 个数量/花色组合，以及连续 20 个逻辑轮次；6 个布局/资源回归、6 个出帧就绪/取消回归和 7 个附件/祝福回归详见上文。逻辑、模拟资源和帧信号测试不等于设备冷启动、性能或实际重玩测试。
- `assembleDebugAndroidTest` 成功；`LaunchSmokeTest` 已编译，未在设备执行。
- 实验 API 检查通过：无活动实验 opt-in。
- SpatialUI verifier：0 errors / 1 warning。唯一 warning 为结果文字使用 `headlineMedium`，依据本次用户要求放大一倍予以保留。动画只用于结果切换，不实现自定义 hover；花瓣 hover 使用 SDK HoverEffectComponent，按钮使用原生 Button。
- `accessibility project-audit`：0 errors / 0 warnings。重复工厂动态 ID 在 Contract 中以初始 `rose.petal.0` 完成声明；实际每片拥有唯一 `rose.petal.<id>`。未声称所有节点的 ROM runtime-audit 已通过。
- `python3 tools/verify_rose_assets.py`：复读实际导出 GLB，12 个独立网格哈希、闭合正反面/厚度边、每片 412 面、基座 1,692 面、三骨骼、归一化混合权重、30 瓣最多 14,052 面均通过。
- Editor 导入与正面/斜侧面离屏截图已成功，检查了宽扇形轮廓、分层、短茎与花芯。`art/rose-production-front.png` 和 `art/rose-production-angle.png` 是当前模型预览，尚不能证明所有随机组合无穿插。

## 环境事实与阻塞

- Root doctor：CLI 0.7.0-beta.5（internal）；general plugin 0.2.0-beta.5；development knowledge 6.2（graph.json 缺失）；Editor 6.2.4；Emulator 未安装。Android SDK 35 与本地 Spatial 6.2.8 缓存可用。CLI freshness 使用已有缓存；插件 freshness 未证实；性能工具依赖有非本次构建必需的 warning。
- `project configure` 失败：`FILE_LOCK_IDENTITY_UNAVAILABLE`，无法在 CLI managed cache 中获取进程锁身份。未修改 Host 配置。
- `project create --template volumetric` 失败：安装的 SDK 缺少 `spatial/volumetric/filelist.txt`。随后只在本工作区恢复了本地已有的 Volumetric 模板结构，并选用完整缓存中的 6.2.8。未改写全局 SDK、知识库或插件目录。
- 知识 MCP 当前为 unavailable；API 依据本地精确 6.2.8 public SDK source/KDoc、技能内指南与编译验证。官方眼手接口参考：<https://developer.picoxr.com/spatial-api/6.1.9/spatialui/foundation/com.pico.spatial.ui.foundation.gesture/detect-spatial-pointer-event.html>，该页面只提供使用模式，不能替代 6.2.8 真机验证。
- `pico-cli device list` 失败：ADB 无法建立 tcp:5037 监听，`could not install smartsocket listener: Operation not permitted`。没有绕过当前会话权限，未安装/启动应用、未获取 crash buffer、未执行 runtime-audit 或眼手动作。
- 本次 root doctor 复核：CLI `0.7.0-beta.5 (internal, 052257d)`，general plugin `pico-general-agentic-tools@0.2.0-beta.5`，SDK `6.2.8`，development knowledge `6.2`，profiling tool `0.3.5`，Emulator `null`，Editor `6.2.4`。CLI freshness 仍取缓存，插件 freshness 未能证实；知识图谱缺失、性能依赖提示和项目未配置状态保留。Editor 子检查创建全局 state lock 遭 EPERM，未重试或修改工具目录。上述缺口不阻塞本次独立的 Gradle 构建及本地 Accessibility 源码审计；知识/Editor/设备能力未据此认定可用。

## 必须继续验收

1. 在具有已授权 ADB 的 PICO OS 6 真机安装并启动，收集启动窗口的新 crash buffer 与应用日志，执行 LaunchSmokeTest。
2. 核验 `inputDevicePose.rawPosition` 在目标 ROM 中的 Global View 坐标语义及转换比例。代码从手部 pose 转换到 SpatialView 米制坐标，不使用投影命中点代替手位移；但现有 public KDoc 没有完整说明 rawPosition 的单位，因此 8 mm/4 cm 的实际物理距离尚未得到设备证据。任意方向的逻辑距离已有单测，仍需目标设备校准。
3. 逐个验证所有方向达到 4 cm 均可摘下、阈值前取消、锁定后移开视线、追踪丢失/恢复、双手输入、末瓣长按和新捏合重玩；重点验证公用纹理跨轮仍有效、骨骼就绪、错误重试恢复 Ready，必要时取新增带 stage 的异常堆栈。复核 80sp 白字无裁切、完成后按钮可点击、重复点击仅创建一轮。当前追踪宽限覆盖无效 pose/指针流中断；还需核验 ROM 丢失事件的实际行为。
4. 确认 SkeletonPoseComponent 导入后的局部姿态语义、实际蒙皮变形、花冠同步，以及 detach 同帧重置不使脱离花瓣跳回。
5. 确认八色 Unlit、背面、独立透明混合/排序、文字对比度与结束微亮；Unlit 的姿态和淡出不能仅凭数值认定通过。
6. 用真实网格验证 17–30 的全部布局、变体组合和保留空间；检查所有花瓣包括末瓣的可达性。当前预算/闭合检查不包含全面的三角形穿插证明。
7. 补全每个正式变体的独立正/右/俯三视与更细腻的烘焙，当前低多边形造型与参考图仍有差距。
8. 完成坐姿/站姿、默认距离、窗口边界、20 轮实际性能/内存与 A11Y runtime-audit。使用原生 Compose semantics 和 ROM UiAutomator 路径，不安装辅助服务。

P1 声音与静音入口未实施；PRD 允许核心体验无声运行。
