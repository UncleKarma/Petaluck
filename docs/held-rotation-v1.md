# 第3项：摘下花瓣后的手部旋转

版本1.0.15/code16；基于第2项提交32ae069。只在 DetachedHeld 阶段应用手部旋转，保持摘取阈值、Direct绝对位移和Gaze首MOVE锚定/相邻增量逻辑。

## 行为与坐标

- 两种捏合均读取 SDK 6.2.8 `SpatialPointerInfo.inputDevicePose.rawRotation.toQuaternion()`，调用 `content.convertRotation(Global, localSpatialCoordinateSpace)`。该版本实现是四元数 `(-x,y,-z,w)`，对应位置坐标的Y反射。`GestureHelper.getValidRotateQuaternion`只校验原始四元数，不做坐标反射，避免重复转换。
- 当前RoseAnchor只有平移和均匀缩放，旋转轴与场景一致。以根实体重挂时保存的完整姿态为基准：`currentHand * inverse(handAtDetach) * petalAtDetach`。四元数归一化，不使用欧拉角差、平滑或限幅，不围绕手部额外公转。
- 捕获指针的每个Moved先观察旋转，再执行位移/脱落，最后应用持有旋转。脱落事件本身只建立基准，因此不把此前手部旋转叠加到已受力花瓣。第二只手、离开碰撞命中和kind变化继续由现有PinchDragTracker处理。
- 位姿缺失/读取或转换异常不阻断位移，保留上次旋转；脱落时没有姿态则以首次有效姿态建立基准，花瓣不跳转。零设备位置视为姿态不可用，合法identity旋转仍可使用。SDK会把非有限/零原始四元数替换为identity，公开API无法区分此回退与真实identity；本版没有宣称能识别这种原生输入异常。
- release先刷新Gaze位移、应用最后旋转，再保存原生`fallStart`完整TRS，随后清理旋转状态。UP/cancel元数据不当作新的拖拽采样，沿用最后接受的拖拽位置/旋转。保留1.2秒下落、侧摆、原附加旋转和淡出公式；暂停/失踪超时/清场走既有取消释放。

## 单测报告

范围：新增`HeldPetalRotation.observe/detach/clear`；用现有真实位移/捕获类验证衔接。测试调用SDK真实纯数学Quat/Vector3，无模拟原生Entity。

缺陷分析：未确认新增逻辑缺陷。候选检查中，观察到旋转基准与乘法顺序明确、missing-at-detach有延迟锚定、释放和换轮清理齐全；无证据把原生姿态语义不确定性当作代码缺陷。原生场景调用、SDK坐标转换执行和画面表现未由JUnit覆盖。

生成用例（8项）：

1. 摘下时保留受力姿态，忽略摘下之前的手部旋转。
2. 非identity基准上，手部差值按场景轴作用于花瓣。
3. XYZ三轴正反方向、跨180°和360°、重复采样不累积漂移。
4. 等价四元数q/-q不改变朝向。
5. 摘下时缺少姿态，首次有效采样不跳转。
6. 持有期间缺少姿态仍可平移，恢复后继续旋转。
7. Direct/Gaze释放的位置和旋转快照不受清理、晚到输入、下一次抓取影响。
8. 只有锁定指针驱动旋转，kind变化与离开命中不破坏归属。

验证：Step5首轮定向命令`./gradlew :app:testDebugUnitTest --tests com.example.flowertears.p18ab52cf.HeldPetalRotationTest`退出0；8项全部通过，无测试修复。随后`./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest`通过，126项/0失败/0错误/0跳过。命令均使用Android Studio JBR。

Step6：无覆盖率门槛且非flux执行，依技能规则跳过，不提供覆盖率百分比。Step7：已执行`utree flush`，退出127，`/Users/bytedance/.local/bin/utree`不存在；仅工具汇总未生成，真实Gradle结果如上。

实验API检查PASS；SpatialUI PASS（0错误，1条既有用户大字号警告）；`git diff --check`通过。

## APK与头显待验

`backups/held-rotation-v1-2026-11-13/RoseTear-1.0.15.apk`

SHA-256：`9e5bab56abfc23c13ee745fe8954b88398719f3fbb07cf44720b4be81a7cd328`

ADB设备列表为空，未安装。需分别用Direct/Gaze验证摘下瞬间不跳转、三轴旋转方向和跟手连续性、纯旋转不移动位置、松手不回到旧朝向，以及丢失跟踪/第二只手的隔离。仍需复测第1项第二轮闪帧与第2项文字位置。现有主机证据不代表头显校准完成。
