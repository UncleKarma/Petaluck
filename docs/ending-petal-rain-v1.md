# endingBGM 与 bundle 花瓣雨

2026-11-12，APK 1.0.8 / versionCode 9。

## 实现

- 将已交付的 `petal-rain.bundle`、`.scenes.json`、`.provenance.json` 原字节放入 APK assets。场景键 `Scenes/PetalRain` 来自 schema 0.1 sidecar。
- `EndingPetalRain.prepare` 在 Ready 之后的可选准备任务中读取 manifest，后台加载 AssetBundle，再用 `loadModelSuspend` 加载场景。验证 SDK 返回的唯一子 scope 的完整顶层名称，再读取原生 ParticleComponent；未知层级直接报可选加载失败，不改花朵状态。
- 每轮预备独立、未挂入 SpatialView、disabled 且不发射的实例。`EndingMusicPlayback` 在实际 AudioTrack.play 返回成功后通知 start(roundId)。特效仅在已准备且轮次匹配时启用，直接挂到 SpatialView，不继承 RoseAnchor 的 -0.10m、缩放或弯曲。
- 三个发射器设置为 `(0,.33,0)` 米、原尺寸 scale 1，顶部距既定 +.35m 容器上沿约 2cm。保留包中粉色透明花瓣、12片/秒、5/6/7秒寿命、首尾各1秒渐隐。未扩展成八花色效果。
- 音乐自然结束、watchdog、异常、暂停、退出调用视觉 stop；接受重玩时即 stop，音乐继续原有1000ms淡出。回调一次性防止旧淡出结束误清下一轮预备实例。clearRound 也清粒子，独立音乐不在该处销毁。
- stop 同步 disable 并 destroy 实例，避免仅关闭 emission 留下存活花瓣。bundle 保留到场景销毁；不可取消的 native 加载结果用 epoch、调用任务和 closed 校验，过期实体销毁后才释放缓存和 bundle。
- 没有增加控件、命中区域或粒子逐片无障碍动作；装饰效果由已有完成状态表达。保留初始 rose.petal.0 的人工完整绑定，仅刷新 Contract 的来源位置与 inventory 锚点。

## 检查

- `assembleDebug` PASS；`testDebugUnitTest assembleDebugAndroidTest` PASS，111 tests / 0 failures / 0 errors。
- 新增3项生命周期回归（包含多分支）：实际音频start在视觉前、同轮只启动一次、fade开始即停且旧fade不重复通知、自然结束/watchdog/暂停清理、原生play失败不启动粒子、原生stop异常仍清视觉。
- installed `check_experimental_api_opt_in.py --target .` PASS；`verify-design-style.sh app --design-color 'roseTextWhite=#FFFFFF'` PASS，0 errors / 1既有用户要求的大字warning。
- `pico-cli app bundle verify app/build/outputs/apk/debug/app-debug.apk`：1 uncompressed AssetBundle。独立ZIP检查method=0、APK内bundle与Editor输出逐字节一致。
- `accessibility project-audit`：0 errors / 0 warnings，保留5个已有所有者及手工花瓣绑定。Ready场景runtime-audit：48节点、未截断，3/3当前阶段节点可见，0 errors。全Contract一次性审核因结果/完成组此时按设计disabled而失败；未为通过检查改变隐藏逻辑。

## 有限真机证据

目标 PICO `PB311XHGKC150135G`，最终APK升级安装Success，launch返回成功。
设备日志：

```text
17:43:54.501 Round assembled id=1 N=28 color=6
17:43:55.307 RoseEntranceFinished session=1
17:43:55.461 PetalRainReady round=1 emitters=3 y=0.33
17:43:55.472 EndingMusicReady frames=639450 durationMs=14500 channels=2
17:45:00.796 PetalAudioStopped reason=completed
```

使用既有公开摘取动作成功摘取petal0。随后27项顺序batch第一步返回`TARGET_ROOT_TIMEOUT`，`performed=false`、0完成，未派发任何剩余摘取。未触发结束音乐，因此没有原生音乐/粒子start或stop的证据，不能将预加载成功描述为联动目视验证。

本次录屏 `.scratch/petal-rain-v1-device.mp4` 实际56.504秒；不作为花瓣雨验收画面。已停止本次录制，`capture camera reset`返回`restored=true`、`injected=false`、`inputSource=swan-tracking`。前后读取crash buffer为空，仅限本次窗口。

## 环境与待验

doctor：CLI 0.7.0-beta.5、general plugin 0.2.0-beta.5、SDK 6.2.8、知识选择6.2、Editor 6.2.4、emulator未安装；Node26.8.1/npm11.19.0。graph.json缺失；Editor CLI状态锁写入EPERM，未修复或重试该写入；使用已交付包和本地确切6.2.8公开源码。CLI提示0.7.1-beta.0，未升级。图谱不可用不阻塞本地Gradle或已验证ADB路径。

头显完整联动仍待佩戴验证：摘完启动、自然结束、重玩淡出立即清空、连续重玩、暂停/退出及加载异常。实际渐隐、透明排序、噪声运动裁切、帧耗时/内存均未验收。sidecar静态bounds=null且自动容器仅.256m见方，不代表粒子包络；本次不采用它缩小既定.60×.70×.40m DefaultWindowContainer。

## 交付校验

- APK SHA-256：`32c7dd3af0330b374d0c313193092cf17c3e09ee93a0f68d6fdec709434724fa`，51,565,292 bytes。
- Bundle SHA-256：`b06853e011a307471dde8133fd0de386e226b07c9cf5a2bf129b481599fc17e9`。
- 独立APK归档：`backups/ending-petal-rain-v1-2026-11-12/RoseTear-1.0.8.apk`。
