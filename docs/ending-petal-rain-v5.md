# 花瓣雨旋转与自然收尾 v5

2026-11-13，APK **1.0.12 / versionCode 13**，revision `drain-tail-v5`。

## 行为

- 三轴角速度范围统一增加25%：X 15–32.5、Y 10–22.5、Z 12.5–27.5°/秒。
- FREE方向为(0,0,1)，variation从1减为0.55；出生X/Y为0，Z为100–220°随机常量，使初始姿态偏向尖端朝上/朝侧，随后保持三维旋转。旧angularSpeed模块继续关闭。
- 总发射率仍为12片/秒。播放后10秒设`isEmitting=false`，保留实体和存活粒子；15秒销毁实例。每片寿命统一5秒：淡入1秒、保持3秒、淡出1秒，因此最后一批有完整淡出时间。
- 14.5秒BGM自然结束或自然结束watchdog不销毁雨；点击「再试一次」立即停雨，并保留原音乐1000ms淡出。暂停、播放失败、轮次清理和销毁仍立即清理。
- `PetalRainSchedule`通过主线程Handler持有10/15秒定时器，取消时递增generation，防止旧回调影响下一轮。即使音乐已结束，重玩/暂停也显式清理雨。

此前粒子持续发射，最长寿命7秒，而音乐结束直接销毁全部实例；因此音乐末尾仍有可见粒子。这次将停发射与清理分开，并缩短寿命以满足15秒总时长。

## Editor与打包

原生Editor回读和保存的USDA均确认三个发射器参数。`rotation-v5-front.png`、`rotation-v5-oblique.png`为实际Editor视口截图，已查看上/侧向尖端和宽面/斜面/细边混合；未统计朝下比例，截图不证明头显连续运动。

Editor stop/play未重置一次性发射时钟，最终资源保留`loop=true`，应用通过公开`isEmitting`接口在10秒截止。单独Editor预览会循环，不代表应用时序。

原生`pack_project`在已有GUI会话打包成功，1场景、3879ms。包15,184 bytes，SHA-256：

`9e698b82efa4f7da8a63001a0050f4f3721e68191c05a286ed437a1864c5ed40`

CRC、`AssetInfo.json`场景键`Scenes/PetalRain`和三个顶层名称通过；不含用户`Hi`场景。Editor导出的schema-0.1清单补充经包内确认的runtimeSceneKey。provenance是调用方编写的原生打包回执，不是托管gate。脚本重开场景被Editor安全策略拒绝，未绕过或声称重载验证。

## 单测范围与缺陷分析

本次覆盖`EndingMusicPlayback.tick/stop/stopVisual`与`PetalRainSchedule.start/cancel`。针对已实现的修订逻辑分析，未发现可证实的新增缺陷；这不代表原生渲染已验收。

新增7项用例：10/15秒边界、重复start不延长、取消后旧回调隔离、尾段暂停、14.5秒自然音乐结束保留尾段，以及音乐自然结束/watchdog和重玩/暂停/淡出中结束的回调区分。两组分别运行通过后，全量**118项单测、0失败、0错误**。

单测技能流程中的`utree`记录同步因本机可执行文件缺失（exit127）未完成；不影响Gradle用例执行。无覆盖率配置，未声称覆盖率百分比。

## 构建与设备

- Android Studio JBR运行`testDebugUnitTest assembleDebug assembleDebugAndroidTest`成功。
- 实验API检查PASS；已安装SpatialUI样式检查0 errors / 1既有大字warning。
- APK内bundle字节与Editor包一致，ZIP method 0（noCompress）。
- `adb install -r`成功；设备package回读versionCode13 / versionName1.0.12。
- 初次安装启动时设备screenOn=false、visibleWindows=0；唤醒后收到首帧，但花朵装配在`load stem`失败：`Stem must have exactly one skinned mesh`。本轮未修改该断言；与已交付1.0.11 APK逐字节对比，花茎GLB相同（SHA-256 `5010f4644821f4c73688c0bf9e5f82a7efc25dc88b8e772a80d8fdce4e4278f1`），原生返回异常的原因尚未定位。强制停止后重启，仍未取得应用操作节点，查询报`No accessibility root matched ... within 2000 ms`，未派发摘取动作。
- 因此本版尚无设备PetalRainReady/Started、10秒EmissionEnded、15秒TailFinished记录，不能宣称已验证实际渐隐、旋转、尖端比例或多轮表现。应用初始化失败同样没有被确认解决。

设备证据：`petal-rain-v5-device.log`。Editor参数：`petal-rain-v5-editor-readback.json`。

APK归档：`backups/petal-rain-drain-v5-2026-11-13/RoseTear-1.0.12.apk`。
APK SHA-256：`dd0b3c422fe372af9325effe35ea25590b2220d41cac9ee28087c90529b2672a`。

待头显恢复可操作状态后验证：完整结束应在10秒停止新生、14.5秒音乐停止后保留最后淡出、15秒清理；第10–15秒点击重玩应立即清理，下一轮预加载不受旧定时器影响。还需佩戴观察连续旋转、透明排序和窗口裁切。
