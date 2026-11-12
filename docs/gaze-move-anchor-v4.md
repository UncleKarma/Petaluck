# Gaze 首移动基准与姿态更新 / 1.0.5

2026-11-12。包名 `com.example.flowertears.p18ab52cf`，versionCode 6，日志 revision `move-anchor-v4`。修改前 Git 基线 `44b848b`。

## 真实设备证据

用户反馈 1.0.4 仍然轻拉不弯、摘下跳几十厘米。此次 ADB 已可用，采集到了真实复现，旧的 ADB 不可用说明不再代表当前状态。

修复前日志保存在 `backups/gaze-move-anchor-v4-2026-11-12/device-before-1.0.4.log`。

- 14:25:16.994 Gaze grab；首个 MOVE 在 14:25:17.566，间隔 572 ms。期间没有进入应用 Moved 路径的记录。
- down raw `(712.55927,997.3746,4855.635)`，首 move raw `(1213.1,1226.1869,4079.283)`，换算后的差值 `(0.20021628,-0.09152492,-0.3105407)` m，约 38 cm。
- 这笔输入直接跨过 4 cm。分段后的阈值向量约 `(0.021039,-0.009618,-0.032632)` m，剩余约 34 cm 立即应用到持有花瓣。不是阈值段重复相加。后续相邻采样是毫米级。
- 同一次 pull 的 before 和 loaded 原生 TRS 完全相同。SDK 6.2.8 `System.dispatchContainerUpdate` 在 `frameSyncPoint=true` 中分发 SceneEvents.Update；TransformComponent 在该阶段缓存写入，`afterFrameUpdate` 批量提交，而 Entity.convertTransformTo 直接读原生状态。原实现帧回调内的写后读因此不能代表刚写入的姿态。

证据确认了异常输入如何传入花瓣，以及原生读写时机冲突。它没有证明 down/move 底层坐标为何不连续，也没有完成 rawPosition 单位/方向标定。

## 修改范围

- `PinchDragTracker`：remote down 继续锁定花瓣、手和来源；第一笔有效非 down 样本返回 `Anchored`，只建立移动流基准，应用位移为零。后续只换算相邻有效样本。重复 down 不推进基准；无效值不建基准；释放无需 pose。若 pending target 到 MOVE 才首次得到有效 pinch，该 MOVE 本身就是基准。
- `RoseScene.pointer`：每个 Gaze Moved 立即按序消费 RemotePetalDrag，与 Direct 输入路径同一阶段。tick 不再消费输入，继续驱动花茎回弹与下落。release 仍有最终 drain。
- 8 mm 净位移死区、4 cm 球面交点、完整 TRS 交接、只加入剩余段和后续增量的算法保持。Direct 仅增加诊断日志；其运动路径保持。
- 增加 grab 原始/局部位置、anchor 前后 raw、Direct total 与两种输入的 reparent 诊断。DEBUG 日志在复测结束后应关闭。

第一笔 MOVE 之前的运动被明确排除。这不是距离限幅或平滑；系统不发移动事件时应用仍无法弯曲，首 MOVE 的真实手部移动也不会被计入。是否产生可感知延迟需复测。后续输入自身若再出现跳变，仍会完整应用。

## 测试范围、缺陷分析、用例与结果

测试流程 Step1 prepare 完成；Step2 Kotlin/Android/Gradle/JUnit4，EXEC_SOURCE 为空；Step3 范围为 PinchDragTracker.update 及既有拖拽契约回归；Step4 缺陷为 down 到首 MOVE 的不连续差值被当作有效运动；Step5 生成→验证→修复完成；Step6 无覆盖率要求/配置，按流程跳过；Step7 已执行 utree flush，但 `/Users/bytedance/.local/bin/utree` 缺失（exit 127）。

新增 3 项测试：

1. 重放上述真实 down/首 MOVE/次 MOVE 数值：首 MOVE 不拉扯或摘取，次 MOVE 保持死区；之后 2 cm 加载、4.1 cm 摘取、停手不漂移、继续移动只增量跟随。
2. 无效首样本、重复 down、仅建一次基准、释放后下一次抓取重新建基准。
3. 建基准前无 pose 松手、pending Other 到 Remote 的捕获及原目标锁定。

维护 10 处既有测试的 remote 基准采样前置条件，保留原阈值、倍率变化、清理和最后一瓣断言。

- 第一轮 `:app:testDebugUnitTest --tests '*PinchDragTrackerTest'`：15 tests，新增日志重放在旧生产逻辑上断言失败，其余通过；证明首样本差值会触发摘取。
- 修改后 tracker 与 RemotePetalDrag 测试通过。随后全量运行发现 IncrementalPetalDragTest 中 3 个旧测试仍将 Anchored 强转 Moved；补齐基准采样后通过。
- 最终 `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest` 成功：**99 tests / 0 failures / 0 errors / 0 skipped**。测试 APK 仅编译，未运行设备测试。
- 本轮未找到此前使用的实验 API / SpatialUI 检查器可执行脚本，不能沿用历史 PASS 作为本轮结果。编译与 diff 检查通过，UI 代码无修改。
- 原始和 raw WAV SHA-256 保持 `ea4338224bc3db7a417aade1cacc897069c9f3ae94302c2ebac433e269368982`；PCM gain 0.025、track gain 1 保持。

JVM 测试不执行原生骨骼或矩阵提交；实际姿态连续性、米制标定、停手稳定性及 Direct 对照需新的设备日志和用户反馈。

## 交付与复测

APK：`backups/gaze-move-anchor-v4-2026-11-12/RoseTear-1.0.5-gaze-move-anchor-v4.apk`

SHA-256：`6f39149f41cdf540df2f5462381985fbcf407046f8675ca1d14af0283494679e`

ADB `install -r` 返回 Success，dumpsys 核验 versionCode=6 / versionName=1.0.5；已发出启动 Activity 命令。这不等于已验证启动无异常或交互已修复。

复测日志：`backups/gaze-move-anchor-v4-2026-11-12/device-after-1.0.5.log`，共 9,127 行，采集已停止，`log.tag.RoseGaze` 已恢复原空值。

用户按慢速横拉、摘下停手、继续移动松手及 Direct 对照复测后反馈：**“看上去行为一致了”**。这是本次有限真机复测的通过反馈，不代表所有方向、全部用户或长时间压力验收。

| Gaze 花瓣 | 阈值前 pull 次数 | strength > 0 次数 | 首次 held 相对 tear 位移 | 换父位置误差 |
| --- | ---: | ---: | ---: | ---: |
| 11 | 48 | 42 | 0.397 mm | 0 |
| 5 | 17 | 15 | 0.401 mm | 0.0000081 mm |
| 10 | 11 | 9 | 0.537 mm | 0.0000070 mm |
| 4 | 10 | 9 | 1.136 mm | 0.0000070 mm |

四次均先收到 appliedDelta=0 的 anchor，再出现连续受力更新。crossing 时 loaded 均已反映新位置，相对 before 分别改变约 0.139 / 2.268 / 2.550 / 2.384 mm。原先首笔约 34 cm 的 held 跳位在这些样本中没有重现。Direct 花瓣 6 的换父位置误差约 0.000007 mm。这里只量化位置误差；完整旋转、缩放记录保存在原日志。

日志未见 FATAL EXCEPTION；不能由有限、过滤后的日志推断绝对无崩溃。停手区间没有独立动作时间标记，因此不虚报静止漂移数值。raw 单位/方向的物理标定及更广泛验收仍未完成。
