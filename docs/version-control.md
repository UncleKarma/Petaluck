# 版本基线与回滚

## ADB 花色调试 1.0.20

新增持续指定后续花色的调试命令，139测试及构建/静态检查通过；已安装并验证广播校验与重启恢复随机。详见 [debug-rose-color.md](debug-rose-color.md)。


## 彩色月季跨层邻接 1.0.19

增加所有层组合的相近方位异色约束，左右邻接保留。136测试与构建/静态检查通过；已安装，白色月季 Ready 场景审核通过，彩色原生观感待复测。详见 [multicolor-neighbors-v2.md](multicolor-neighbors-v2.md)。


## 彩色月季 1.0.18

新增第九种等概率花色、逐瓣随机基础色和八色花瓣雨；135测试、构建和静态检查通过，当前无连接设备，原生效果待验证。详见 [multicolor-v1.md](multicolor-v1.md)。


## 第二轮闪帧修订 v2

APK1.0.17/code18另存于`backups/completion-buffer-v2-2026-11-13/`并附SHA-256。132主机测试和构建通过；本增量独立提交为`6e58860`；实机闪帧消失仍待验证。见 [completion-buffer-v2.md](completion-buffer-v2.md)。


当前基线内容为「一瓣欢喜 / Petaluck」：中英文应用名、PICO 图标、分层花瓣、任意方向 4 cm 摘取、首帧与二维附件绑定修复、12 条随机居中祝福，以及祝福 600 ms / 重玩按钮 450 ms 淡入。

2026-11-16：英文和默认系统语言的启动器显示名改为 `Petaluck`；中文各地区仍显示「一瓣欢喜」。Manifest 继续引用 `@string/app_name`，开发包名及资源路径不变。

## 当前状态

2026-11-12 已确认工程存在 Git，分支为 `main`，初始提交 `5879919 Initial commit`。本轮开始时工作区干净。2026-11-10 创建 `.git` 被拒绝的记录属于历史状态；下文各旧快照中的“没有 commit”描述的是当时的交付状态。

源码、用户参考、建模工具、正式资产、Editor 审阅源和验证记录已准备完整本地快照，见 `backups/baseline-2026-11-10/`。源码归档、APK、文件清单与 SHA-256 校验信息放在一起。缓存、构建目录、本机 `local.properties`、临时工作数据和私有密钥配置不进入源码归档或 Git；Gradle Wrapper JAR 是构建所需源码设施，会保留。

## 查看已有 Git 基线

无需重新初始化。可运行：

```bash
git --no-optional-locks status --short
git log -10 --oneline
git show 5879919 --stat
```

每次仅暂存该增量文件；不包含 build、.gradle、.kotlin、local.properties、backups 或 APK。不改写 Git 配置。若后续元数据写入仍被会话权限拒绝，记录失败并由本机终端完成，不改用其它 Git 目录绕过限制。未确认存在基线 tag，查看旧版优先使用明确的 commit ID。

## 后续每轮修改

每项用户请求单独提交，提交说明包含实际行为变化和验证结果。保留当前改动记录；修改开始前检查工作区，不混入用户并行修改，不改写既有提交。较大里程碑创建独立 tag；可安装 APK 单独保存并记录对应 commit/tag 和 SHA-256。

### 2026-11-13：跟手旋转轴向修订

1.0.16 / code17：移除设备旋转上的额外X/Z反转，保持Y及原相对旋转/位移/下落路径。126项既有测试、APK构建和静态检查通过，已安装并启动；实际三轴方向待复测。归档`backups/held-rotation-v2-2026-11-13/RoseTear-1.0.16.apk`，SHA-256 `1a71e7153eb406a7d56e903ff410184bbc76b6c9120fd7662a8de7eb698c7156`。详见`docs/held-rotation-v2.md`。

### 2026-11-13：第3项脱落花瓣跟手旋转

1.0.15 / code16：Direct/Gaze共用场景坐标手部旋转，以摘下姿态建立相对旋转基准，释放从最终TRS开始原有下落淡出。126项测试（新增8项）、开发/测试APK和必要静态检查通过。无ADB设备，原生旋转方向与连续性待验。归档`backups/held-rotation-v1-2026-11-13/RoseTear-1.0.15.apk`，SHA-256 `9e5bab56abfc23c13ee745fe8954b88398719f3fbb07cf44720b4be81a7cd328`。详见`docs/held-rotation-v1.md`。

### 2026-11-13：第2项文字位置调整

1.0.14 / code15：喜欢/不喜欢下移8cm，祝福与重玩按钮整体下移5cm。118项回归测试、开发/测试APK及实验API、SpatialUI检查通过。无ADB设备，实机位置待验。归档`backups/panel-offset-v1-2026-11-13/RoseTear-1.0.14.apk`，SHA-256 `cd0af129cdfee3f99271dbf4e7b8e934bbb987da700d6476df89999f8ad69ddc`。详见`docs/panel-offset-v1.md`。

### 2026-11-13：第二轮完成面板闪帧修订

1.0.13 / code14：原生完成面板先以opacity0启用，等待本轮透明帧提交后再显示并启动600ms/450ms淡入。118项现有回归测试、构建及静态检查通过；无ADB设备，未安装和完成原生闪帧复测。APK归档`backups/completion-buffer-v1-2026-11-13/RoseTear-1.0.13.apk`，SHA-256 `8f871fc0b71973292f7a5c39b2a789463c69bc11c287bbd1738f39f5e77dcc18`。详见`docs/completion-buffer-v1.md`。

### 2026-11-13：花瓣雨自然收尾与旋转调整

1.0.12 / code13：三轴角速度增加25%，出生方向偏向尖端朝上/朝侧；10秒停发射，15秒清理，每片5秒并保留末尾1秒淡出，BGM自然结束不提前清理。118单测、构建、实验API/SpatialUI及APK内包核验通过，已安装。设备初始化遇到未返回唯一蒙皮网格，完整原生播放仍阻塞，详见`docs/ending-petal-rain-v5.md`。APK归档`backups/petal-rain-drain-v5-2026-11-13/RoseTear-1.0.12.apk`，SHA-256 `dd0b3c422fe372af9325effe35ea25590b2220d41cac9ee28087c90529b2672a`。

### 2026-11-13：花瓣雨取消朝向相机

1.0.10 / code11：原生Editor改FREE朝向、随机XYZ曲线翻转/摇摆，关闭旧匀速旋转并保留双面Unlit及同花色联动；重打bundle。111单测、构建/静态检查、Editor正侧面和参数检查通过；真机确认预加载、播放及自然结束清理。APK另存 `backups/petal-rain-rotation-v3-2026-11-13/RoseTear-1.0.10.apk`，SHA-256 `ddd531f4f33e89562c8f04dc55eda21c001542c9a2cc518edee9f7e6e9182cc7`。详情 `docs/ending-petal-rain-v3.md`。

### 2026-11-13：花瓣雨跟随本轮花色

1.0.9 / code10：使用本轮外/中/内层albedo渲染透明花瓣精灵，三张纹理受原生生命周期管理；每轮独立随机微风，保留Editor Billboard/Unlit、首尾各1秒渐隐及旋转。111单测、构建、必要静态检查和设备24贴图测试通过。已安装，启动后容器隐藏，原生动态播放待验。APK另存 `backups/petal-rain-colors-v2-2026-11-13/RoseTear-1.0.9.apk`，SHA-256 `4897abd167b75bb9111a36e1c4231b12419d06b030b3d015759f18b87669d0fd`。详见 `docs/ending-petal-rain-v2.md`。

### 2026-11-12：结束音乐花瓣雨接入

1.0.8 / code 9：运行时加载已交付 Editor bundle，结束音乐成功开始时启用顶部粉色花瓣雨，音乐结束、重玩开始淡出、暂停或清理时销毁特效实例。111项单测、开发/测试APK、实验API、SpatialUI及bundle未压缩检查通过；头显确认3发射器预加载。完整联动自动验收被ROM TARGET_ROOT_TIMEOUT阻断，详见 `docs/ending-petal-rain-v1.md`。APK另存 `backups/ending-petal-rain-v1-2026-11-12/RoseTear-1.0.8.apk`，SHA-256 `32c7dd3af0330b374d0c313193092cf17c3e09ee93a0f68d6fdec709434724fa`。

### 2026-11-10：自然花瓣与茎叶基础贴图

原基线快照保持不动。本次新增 `botanical-basecolor-v2`，包括八色独立花瓣底图、茎叶花蕊图集、UV 修正、按颜色保留共享纹理、可重建源图/提示词和离线对照。模型三角形位置、骨骼与蒙皮对照原归档验证一致；26 单测、APK 构建、资产/贴图和必要源码检查通过，实际头显待验证。修改与验证记录在 `docs/verification.md`、`docs/materials-v2-review.json`。

当前会话仍没有可用 Git 仓库，未提交 commit；新完整源码和 APK 单独保存于 `backups/materials-v2-2026-11-10/`，配有逐文件清单和 SHA-256。该快照保留本次成果；`baseline-2026-11-10/` 保留修改前状态。之后在本机初始化 Git 时，当前工作树包含本次纹理更新，初始提交即对应更新后的状态，不能将它称为更新前的旧基线。

## 查看旧版与回滚

祝福音乐修订 APK 1.0.7 / code 8 保存在 `backups/ending-bgm-v1-2026-11-12/`，附 SHA-256。每轮祝福播放 endingBGM，重玩时 1 秒淡出；108 项单测与构建、实验 API / 样式检查通过，头显音频验证待完成。见 `docs/ending-bgm-v1.md`。

重玩文字闪帧修订 APK 1.0.6 / code 7 保存在 `backups/replay-panel-v1-2026-11-12/`，附 SHA-256。基于 `536dfd7`，固定提示与完成区的原生面板位置和尺寸，重开同步隐藏旧文字。99 项单测及构建通过，用户头显复测确认错位闪帧已消失（2026-11-12），见 `docs/replay-panel-v1.md`。

Gaze 首移动基准修订 APK 1.0.5 / code 6 保存在 `backups/gaze-move-anchor-v4-2026-11-12/`，含 SHA-256 与前后设备日志。基于 `44b848b` 修订；99 项单测和构建通过，已安装，用户有限头显复测反馈两种捏合行为一致；4 次 Gaze 的日志未再出现原始首笔大跳位。详见 `docs/gaze-move-anchor-v4.md`，源码提交以实际 Git 记录为准。

GazePinch 全程有序增量版 APK 1.0.4 / code 5 保存在 `backups/gaze-drag-ordered-v3-2026-11-12/`，附 SHA-256。以 `5879919` 为修改前基线；本轮源码使用后续独立提交保存（以 Git 实际结果为准）。96 项单测、开发/测试 APK、实验 API 和样式检查通过；真实头显输入校准与显示仍待确认。详见 `docs/gaze-drag-ordered-v3.md`。

PCM v3 再次减半修订保存于 `backups/petal-audio-pcm-v3-2026-11-12/`，含源码、APK、清单和 SHA-256。PCM gain 0.05 → 0.025；APK 1.0.3 / code 4；86 项单测、开发/测试 APK 和交付检查通过，头显试听待验证。当前 WAV 和交互代码保持，旧快照保留。详见 `docs/petal-audio-pcm-v3-review.json`；无 Git commit。

PCM 增益减半修订保存于 `backups/petal-audio-pcm-v2-2026-11-12/`，包含源码归档、APK、清单和 SHA-256。当前音效文件保持，PCM gain 0.10 → 0.05，APK 1.0.2 / code 3；86 项单测与开发/测试 APK 构建通过，头显试听待验证。已有测试改为兼容当前 WAV 的 LIST 元数据。详见 `docs/petal-audio-pcm-v2-review.json`；无 Git commit，旧快照保留。

PCM 输入衰减修订保存于 `backups/petal-audio-pcm-v1-2026-11-12/`，含源码、APK、逐文件清单和 SHA-256。音效采样在写入前缩至 0.10，音轨 gain 为 1；实际生产缓冲测得约 -20 dB RMS，旧版 setVolume(0.1) 的原生效果仍未知。86 项单测、开发/测试 APK 编译通过，APK 版本 1.0.1 / code 2；头显输出待验证。拖拽与场景保持上一 v2 内容。详见 `docs/petal-audio-pcm-v1-review.json`。无 Git commit，旧快照均保留。

注视捏合逐帧增量修订 v2 保存于 `backups/gaze-drag-volume-v2-2026-11-12/`，含独立源码、APK、逐文件清单和 SHA-256。remote 脱落后改为当前位置累加相邻手部 delta，场景帧只应用一次，release 前刷新 pending；DirectPinch 原路径保持。音轨 gain 0.25 → 0.10（再降低 60%）。81 项单测、开发/测试 APK 编译通过，头显效果待确认。用户反馈上一 v1 未解决 gaze 问题，该快照仍保留；详见 `docs/gaze-drag-volume-v2-review.json`。没有 Git commit。

音量 25% 与 gazePinch 相对位移修订保存于 `backups/gaze-drag-volume-v1-2026-11-12/`，含独立源码归档、APK、逐文件清单和 SHA-256。注视捏合改为先算原始设备位移再转换单位，避开视图原点变化；DirectPinch 原路径保留。75 项单测、APK 和测试 APK 编译通过，头显拖拽/试听待验证。上一音频版保留不动。详见 `docs/gaze-drag-volume-v1-review.json`。Git 尚未初始化，没有 commit。

系统背景音并行播放修订保存于 `backups/petal-audio-mixing-v1-2026-11-12/`，含独立源码归档、APK、逐文件清单和 SHA-256。以 OBJECT 空间短音轨替换旧媒体控制器，播完主动停止；72 项主机单测、APK 和测试 APK 编译通过，头显混音验证待完成。上一持有拖拽版本保留不动。具体变更与证据见 `docs/petal-audio-mixing-v1-review.json`。Git 尚未初始化，没有 commit。

脱离后持续跟手修订保存于 `backups/held-petal-drag-v1-2026-11-11/`，含源码、APK、逐文件清单与 SHA-256。上一入场版本保留在 `backups/rose-entrance-v1-2026-11-11/`。

整体下移与缩放淡入入场版保存于 `backups/rose-entrance-v1-2026-11-11/`，含完整源码、APK、逐文件清单与 SHA-256。保留本轮开始前用户更新的音效文件；上一版在 `backups/petal-audio-startup-fix-2026-11-11/`。

音效阻断花朵启动的回归修复保存于 `backups/petal-audio-startup-fix-2026-11-11/`，含源码、APK、清单和 SHA-256。上一音效版仍保留在 `backups/petal-spatial-audio-2026-11-11/`，其头显启动失败反馈与修订说明见 `docs/verification.md`；布局已获用户认可的无音效版保存在 `backups/interleaved-layout-v6-2026-11-11/`。

花瓣脱离空间音效修订保存于 `backups/petal-spatial-audio-2026-11-11/`，含完整源码、APK、逐文件清单与 SHA-256。上一花瓣布局版本保存在 `backups/interleaved-layout-v6-2026-11-11/`。当前仍通过独立快照保存历史，没有 Git 提交。

跨层收拢与舒展修订保存于 `backups/interleaved-layout-v6-2026-11-11/`，含完整源码、APK、逐文件清单与 SHA-256。上一版保存在 `backups/natural-layout-v5-2026-11-11/`。

外层包曲与随机布局修订保存于 `backups/natural-layout-v5-2026-11-11/`，包含完整源码、APK、逐文件清单与 SHA-256。上一中内层修订在 `backups/cupped-layout-v4-2026-11-11/`。

中、内层包拢修订保存于 `backups/cupped-layout-v4-2026-11-11/`，包含源码、APK、逐文件清单与 SHA-256。上一版同向侧倾布局保留在 `backups/seated-layout-v3-2026-11-11/`。

花瓣同层叠压与根部接合版本保存于 `backups/seated-layout-v3-2026-11-11/`，含完整源码、APK、逐文件清单和 SHA-256。当前仍通过文件快照保存版本历史。

多轮结束按钮修复与逐瓣纹理变化版保存于 `backups/replay-petal-variation-v6-2026-11-11/`，包含源码归档、APK、逐文件清单和 SHA-256。当前会话仍无法写入 Git 元数据。

轻快回弹、三轮花色避重和局部次色版保存在 `backups/replay-palettes-v5-2026-11-11/`，含源码、APK、逐文件清单与 SHA-256。

捏合受力与花茎回弹版保存于 `backups/stem-rebound-2026-11-11/`，含源码归档、APK、逐文件清单和 SHA-256。检查记录见 `docs/verification.md`。

花茎与花芯细化版保存于 `backups/stem-core-v5-2026-11-11/`，含源码归档、APK、逐文件清单和 SHA-256。检查记录见 `docs/stem-core-v5-review.json` 与 `docs/verification.md`。当前会话仍无法写入 Git 元数据。

DirectPinch 拖拽修订另存于 `backups/direct-pinch-2026-11-10/`：完整源码、APK、逐文件清单和 SHA-256。它保留 v4 的全部模型/贴图，只调整输入捕获、坐标来源与教学提示；检查记录见 `docs/direct-pinch-review.json`、`docs/verification.md`。旧 v4 和更早快照保持不变；当前仍未建立 Git commit。

轻微承重花茎、奶油黄替换蓝色妖姬、八色独立三层纹理版本保存于 `backups/layered-v4-2026-11-10/`，包含完整源码、APK、清单和 SHA-256。上一 S 形花茎版本在 `backups/stem-v3-2026-11-10/`，可在独立目录打开对照。该版检查见 `docs/layered-v4-review.json` 和 `docs/verification.md`。

本次自然花茎与花冠协调更新独立保存于 `backups/stem-v3-2026-11-10/`，源码归档与 APK 都有 SHA-256 和逐文件清单。上一材质版本在 `backups/materials-v2-2026-11-10/`，最初基线在 `backups/baseline-2026-11-10/`。本次改动与检查见 `docs/stem-v3-review.json`、`docs/verification.md`；当前 Git 限制仍未改变，没有伪称提交成功。

优先在独立工作目录打开旧版，避免覆盖当前未提交内容。建立基线后可在本机终端运行：

```bash
git log --oneline --decorate
git show 5879919 --stat
git worktree add --detach ../RoseTear-baseline-review 5879919
```

检查和构建旧版时需在新目录配置本机 Android SDK 路径；`local.properties` 不在 Git 中。若确认要撤销某项后续修改，优先使用 `git revert <commit>` 生成新的撤销提交，保留完整历史。不要在有未保存改动时直接 hard reset 或 clean。未经用户要求不推送远端，也不把回滚等同于改写历史。

基线之前只有部分临时文件备份与变更说明，没有每轮完整工程历史；这些旧版不能保证完整恢复。本次快照和将建立的基线只代表当前实际文件状态，不表示头显验收或无缺陷版本。
