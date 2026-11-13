# 跟手旋转轴向修订

用户实机反馈1.0.15摘下花瓣后部分旋转轴方向相反；该反馈未明确标注具体轴。头显包信息确认当时版本为code16/1.0.15。

检查SDK 6.2.8实际源码：

- `SpatialPointerEventDetector`读取设备四元数交给`Rotation3D`；`GestureHelper.getValidRotateQuaternion`执行有限性/模长校验。
- `SpatialCoordinateSpaceConverterImpl.convertRotation`调用`convertRotationBetweenLeftRightHandCoordinate`，实际返回`Quat(-x,y,-z,w)`。
- v1根据设备位置的view坐标推定旋转也需要这个反射。主机旋转单测从已转换的Quat开始，没有验证这项输入语义假设。

本次直接采用归一化的`inputDevicePose.rawRotation`，撤销额外X/Z反射，两种捏合共用同一修订。对于单轴输入，相比v1，X/Z响应反向、Y保持；复合旋转继续使用四元数乘法，不使用欧拉角或逐轴角度叠加。

这项修改依据用户反馈和可确认的SDK转换行为，尚未通过物理三轴动作证明设备四元数的完整坐标契约。不能把“已移除反射”表述为“三轴实机校准已完成”。

保持`currentHand * inverse(detachHand) * detachPetal`、脱落锚定、缺失姿态降级、指针归属、现有位移转换和最终TRS下落。增加DEBUG输入四元数日志，输入/输出统一标记`device-rotation-v2`；没有更改设备日志属性、系统设置或手眼交互模式。

验证：使用Android Studio JBR执行`./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest`，126项既有测试全部通过，0失败/0错误/0跳过。包含已有8项旋转算法及Direct/Gaze衔接测试；本次不新增镜像实现的单测，原生输入轴方向仍属于实机检查。实验API检查PASS，SpatialUI PASS（0错误、1条既有大字号警告），`git diff --check`通过。

安装：`adb install -r`返回Success；包信息确认为code17/1.0.16，更新时间2026-11-13 13:45:05。`am start -W`返回Status ok，后续进程存在。未观察到可用于验证手势的应用日志，本轮没有采集受控三轴动作，不能据此声称画面或跟手方向已验证。

APK：`backups/held-rotation-v2-2026-11-13/RoseTear-1.0.16.apk`。

SHA-256：`1a71e7153eb406a7d56e903ff410184bbc76b6c9120fd7662a8de7eb698c7156`。

待复测：Direct/Gaze分别摘下花瓣后做上下翻腕、左右转腕、手掌平面内旋转；检查同向响应，以及松手时不回跳。若仍有轴反向，应基于具体动作及原始/输出四元数继续校准。
