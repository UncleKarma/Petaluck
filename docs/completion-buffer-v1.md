# 第二轮结尾闪帧：完成面板缓冲门控

本次三项请求中的第1项。构建版本 **1.0.13 / code14**。
第2项文字位置和第3项手部旋转尚未修改，按用户要求等本项验证并提交后继续。

## 原因与修订

源码存在一个可确认的时序窗口：`syncPanels`在进入Completing时立即重新启用`rose_completion`，但Compose稍后才重组、绘制新一轮的祝福与零透明度。原生面板被禁用时可能保留上一轮最后的非透明缓冲，因此第二轮重新启用可能先显示旧帧。此解释与用户现象一致，但未通过头显逐帧录制确认所有闪烁来源。

修订流程：

1. 完成面板绑定时安装`OpacityControllerComponent(0)`；进入完成态时，先保持原生透明，再启用面板以便新内容绘制。
2. 本轮祝福和按钮在Compose中同步以alpha0绘制，尚不启动淡入。
3. `CompletionFrameBarrier`使用该附件LocalView的OnDrawListener和Android公开的`registerFrameCommitCallback`等待透明帧提交；回调切回主线程，由当前roundId和生命周期检查确认。
4. 提交确认后原生opacity恢复1，祝福开始600ms淡入；Completed后的重玩按钮继续450ms淡入，按钮操作及子语义仍等待淡入完成。
5. 新轮次、离开完成态和scene销毁清除确认状态；effect释放时撤销draw/commit监听并使已排队回调失效。

保持固定面板、现有位置和大小；不依赖固定延迟或假设若干场景帧后缓冲必然就绪。SDK公开OpacityController描述覆盖实体及子实体；附件实际原生遮罩与提交回调配合仍需设备确认。如果附件没有硬件渲染，记录错误并保持遮罩，不以旧缓冲兜底。

## 验证

- Android Studio JBR：`testDebugUnitTest assembleDebug assembleDebugAndroidTest`通过。
- **118项现有主机单测，0失败/错误/跳过**。这些回归用例不验证Android绘制提交或原生面板行为；未把它们当作闪烁消失的证据。
- 实验API opt-in检查PASS。
- 已安装SpatialUI样式检查PASS：0 errors / 1既有大字warning。保留用户指定的80/104sp字级。
- `git diff --check`通过。
- `adb devices`为空。本版未安装，未执行第二/第三轮结束及暂停恢复的实机验证；上一版初始化失败的原因没有在本次确认解决。

## SpatialUI约束

继续使用`com.pico.spatial.ui.design.Text/Button/ButtonDefaults`与入口现有PicoTheme；`roseTextWhite=#FFFFFFFF`是应用文本token，不替换原生ColorScheme。原生Button拥有前景色、hover和反馈，无自定义hover动画。默认容器保留已配置的透明背景和manifest关闭系统玻璃；结构Box/Column不增加背景、边框或材质。祝福与按钮仍使用现有同一面板布局。

## 交付与提交状态

APK：`backups/completion-buffer-v1-2026-11-13/RoseTear-1.0.13.apk`

SHA-256：`8f871fc0b71973292f7a5c39b2a789463c69bc11c287bbd1738f39f5e77dcc18`

上一轮花瓣雨v5已独立提交为`c08fe85`，本项使用独立增量提交。用户已明确授权删除`.git/HEAD.lock`；本机Shell删除返回Cross-device link，通过工作区文件编辑工具清理后成功提交上一增量。Git提交后的锁清理仍可能遇到相同文件系统错误；以实际HEAD和提交记录验证成功，不因清理告警重复提交。

待验证：至少连续三轮，结束第一帧无旧祝福/按钮；600ms/450ms淡入稳定；按钮完全显示前不可触发；暂停/恢复不出现旧帧或隐藏不恢复；确认日志每轮一次`CompletionFrameCommitted ... revision=buffer-gate-v1`。
