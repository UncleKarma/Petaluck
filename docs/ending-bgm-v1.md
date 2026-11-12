# 祝福音乐与重玩淡出

2026-11-12，APK 1.0.7 / versionCode 8，开发包名保持不变。

末瓣松开、进入 Completing 并发布祝福的同一次 UI 状态更新启动 endingBGM，一轮播放一次；Completed、重绘不会重播。点击有效的「再试一次」立即开始新花装配，仍在播放的音乐以线性增益在 1000 ms 内降到零并停止；自然结束则直接停止。重复或旧轮次按钮回调不会延长淡出或重开音乐。新一轮祝福从曲首、unity gain 播放。

源 `endingBGM.wav` 原字节复制为 `res/raw/ending_bgm.wav`，保留双声道 PCM16 / 44.1 kHz / 639450 帧 / 14.5 秒；SHA-256 `685cf1cc05d88a32ac7152da2ad204364cc61c7b772a2e68c8d412823df9ca7d`。APK 中 WAV 不压缩，解包核对 BGM 与 petalDrop 均和用户原件一致。原 petalDrop PCM gain 0.025、OBJECT 空间音轨保持不变。

`EndingMusic` 是 scene 生命周期所有的独立立体声 AudioTrack，MODE_STATIC、USAGE_GAME / CONTENT_TYPE_MUSIC，不绑定花朵实体、不循环。花朵 Ready 后作为可选音频准备，IO 线程读取并解析 WAV；失败只记录日志，不改变花朵状态。跨轮复用一个约 2.44 MiB PCM 音轨；解码临时数组不缓存。重玩清理花朵不会关闭音乐。主线程 Handler 约每 16 ms 检查播放头，以单调时钟推进淡出，不依赖 Loading 时被暂停的场景 tick；到达末帧或时长 + 250 ms 看门狗时 stop。暂停/退出取消回调并立即停止，不延迟恢复旧曲；退出释放音轨。没有音频焦点请求、MediaSession 或系统音量改动。暂停导致最后持有花瓣取消释放时不新开音乐。

PCM 解码器增加显式双声道选项，默认仍只接受 mono，按声道数验证 block alignment、byte rate、数据帧和时长。音乐使用未衰减原始 PCM；淡出仅控制这条独立 AudioTrack 的 gain，下一次播放恢复 1。Android API 依据：[AudioTrack](https://developer.android.com/reference/android/media/AudioTrack)。没有引入新的 Spatial API；项目仍使用 `gradle/libs.versions.toml` 的 BOM 6.2.8，工程缺少 `.pico-env.json`，未使用不匹配的知识库作 API 依据。

验证：

- 108/108 主机单测通过，新增 9 项覆盖末瓣释放/祝福时序、每轮一次、1 秒淡出、重复重玩、自然结束与淡出中结束、新轮 unity gain、暂停清理、播放头看门狗、实际 WAV 保真与立体声帧长、截断拒绝。
- `testDebugUnitTest assembleDebugAndroidTest` 与 `assembleDebug` 成功；仪器测试仅编译，未在设备执行。
- 安装版本 0.7.0-beta.5 的 `check_experimental_api_opt_in.py --target .` PASS：没有活跃实验 opt-in。
- 同版本 `verify-design-style.sh app --design-color 'roseTextWhite=#FFFFFF'` PASS：0 errors / 1 既有大字 warning（用户指定祝福/结果字号）；UI 未修改。
- APK、哈希独立保存于 `backups/ending-bgm-v1-2026-11-12/`；机器可读摘要见 `ending-bgm-v1-review.json`。

待头显试听：音乐与祝福实际出屏的同步、原始音量是否合适、系统背景音共存、1 秒淡出的原生效果、跨轮与暂停/退出声音清理。源码与主机测试不证明原生音频输出已经通过；本轮未安装或声称头显验收。
