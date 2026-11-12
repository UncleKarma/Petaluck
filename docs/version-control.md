# 版本基线与回滚

当前基线内容为「一瓣欢喜 / RoseTear」：中英文应用名、PICO 图标、分层花瓣、任意方向 4 cm 摘取、首帧与二维附件绑定修复、12 条随机居中祝福，以及祝福 600 ms / 重玩按钮 450 ms 淡入。

## 当前状态

2026-11-10 在本工程执行 `git init -b main`，创建 `.git` 被当前会话文件权限拒绝：`Operation not permitted`。没有 Git 提交或 tag 创建成功，也没有改用其它 Git 目录绕过限制。

源码、用户参考、建模工具、正式资产、Editor 审阅源和验证记录已准备完整本地快照，见 `backups/baseline-2026-11-10/`。源码归档、APK、文件清单与 SHA-256 校验信息放在一起。缓存、构建目录、本机 `local.properties`、临时工作数据和私有密钥配置不进入源码归档或 Git；Gradle Wrapper JAR 是构建所需源码设施，会保留。

## 在本机终端完成 Git 基线

在本机终端运行（不是当前受限会话）：

```bash
cd /Volumes/ZCQThunderbolt4SSD/MyProjects/VibeCodingApp
git init -b main
git add .
git status --short
git diff --cached --stat
git commit -m "chore: establish RoseTear baseline"
git tag -a baseline-2026-11-10 -m "一瓣欢喜 / RoseTear baseline"
git status --short
git log -1 --oneline
```

检查暂存清单后再执行 commit；应包含源码、资产、原始参考和文档，不应包含 build、.gradle、.kotlin、local.properties、backups 或 APK。当前已有 Git 作者配置，不需要改写全局配置。若之后仍处于同样的 `.git` 写入权限限制，提交和 tag 操作也需要在本机终端完成。

## 后续每轮修改

每项用户请求单独提交，提交说明包含实际行为变化和验证结果。保留当前改动记录；修改开始前检查工作区，不混入用户并行修改，不改写既有提交。较大里程碑创建独立 tag；可安装 APK 单独保存并记录对应 commit/tag 和 SHA-256。

### 2026-11-10：自然花瓣与茎叶基础贴图

原基线快照保持不动。本次新增 `botanical-basecolor-v2`，包括八色独立花瓣底图、茎叶花蕊图集、UV 修正、按颜色保留共享纹理、可重建源图/提示词和离线对照。模型三角形位置、骨骼与蒙皮对照原归档验证一致；26 单测、APK 构建、资产/贴图和必要源码检查通过，实际头显待验证。修改与验证记录在 `docs/verification.md`、`docs/materials-v2-review.json`。

当前会话仍没有可用 Git 仓库，未提交 commit；新完整源码和 APK 单独保存于 `backups/materials-v2-2026-11-10/`，配有逐文件清单和 SHA-256。该快照保留本次成果；`baseline-2026-11-10/` 保留修改前状态。之后在本机初始化 Git 时，当前工作树包含本次纹理更新，初始提交即对应更新后的状态，不能将它称为更新前的旧基线。

## 查看旧版与回滚

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
git show baseline-2026-11-10 --stat
git worktree add --detach ../RoseTear-baseline-review baseline-2026-11-10
```

检查和构建旧版时需在新目录配置本机 Android SDK 路径；`local.properties` 不在 Git 中。若确认要撤销某项后续修改，优先使用 `git revert <commit>` 生成新的撤销提交，保留完整历史。不要在有未保存改动时直接 hard reset 或 clean。未经用户要求不推送远端，也不把回滚等同于改写历史。

基线之前只有部分临时文件备份与变更说明，没有每轮完整工程历史；这些旧版不能保证完整恢复。本次快照和将建立的基线只代表当前实际文件状态，不表示头显验收或无缺陷版本。
