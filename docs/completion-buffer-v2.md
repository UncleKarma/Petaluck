# 第二轮结束闪帧：退出清空与实际绘制门控 v2

2026-11-13，APK **1.0.17 / code18**。

用户反馈第二次摘完花瓣仍闪一帧，第一次没有。尚未取得用户对闪现内容的补充描述，也没有成功复现的逐帧视频，因此本次不能宣称所有闪帧来源均已确认或原生问题已消失。

## 源码依据与修改

v1在重玩进入Loading时立即禁用完成附件，没有等其空白内容绘制/提交。其硬件提交监听挂在ViewTree的OnDraw通知上，通知本身不能证明完成区域当前的绘制内容；Android提交也不是空间合成器已展示该buffer的证明。SDK6.2.8的AttachmentPanel按固定ID缓存并复用owner及ComposeView。以上存在保留上一轮非透明buffer的路径，与第二轮才出现的现象吻合，仍是待头显确认的原因假设。

- 保留固定`rose_completion`、560×384dp、(0,-.05,.12)m，不移动或新建逐轮surface。
- 新增纯状态机`CompletionBufferGate`：进入完成态请求空白提交，确认后恢复native opacity并启动原有淡入；离开完成态先撤下祝福/按钮和语义，保持native opacity0，等待空白提交后才disable。
- 空白绘制从面板最外层`drawWithContent`发起，pending时不调用`drawContent`，因此不重放旧子层；在该绘制过程中注册硬件frame commit，回调post主线程。
- 清空提交不再位于`if(completing)`之内，Loading期间也能完成。Loading roundId从0变到新轮次不会取消清空请求。serial隔离旧owner、旧轮次、重复和晚到回调。
- 不增加固定延迟或scene帧等待。Completing→Completed保留600ms祝福/450ms按钮淡入；按钮仍等完全可见才可操作。input、音频、粒子和文本位置不变。
- 清空期间短暂保留已遮罩的owner用于出帧，提交后停用；这是inactive面板立即禁用规则的明确例外，不永久保留透明命中面。

## 验证

- Android Studio JBR：`./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest`成功。
- **132 tests，0 failure/error/skipped**；新增6项覆盖三轮进出、Completed延续、Loading ID变动、晚到清空、提交前退出、owner替换和初始隐藏。
- 实验API检查PASS；SpatialUI PASS（0 errors / 1既有大字warning，保留用户指定80/104sp）；源码A11Y audit 0 errors/warnings，保留动态petal工厂的已完成绑定。
- APK bundle verify：1个bundle，ZIP stored。
- 主机测试验证状态顺序，不能验证Android硬件回调或空间合成器时序。

## 设备与限制

目标 `PB311XHGKC150135G`。修复版安装Success，launch命令成功，app info核验versionName=1.0.17/code18，进程28509运行。新版18:57:55也收到onStop/onSpaceViewHide，未进入可验证的完成态；日志见`docs/completion-buffer-v2-device.log`。修改前1.0.16启动后约0.2秒系统将container设为visible=false，随即onStop/onSpaceViewHide；runtime-audit返回`TARGET_ROOT_TIMEOUT`，无法摘取到第二轮。崩溃缓冲含16:44旧的`com.pico.spatial.runtime` SIGSEGV，不是本次修复版的崩溃证据。未改系统tracking/相机或清空设备数据。

环境doctor：CLI0.7.0-beta.5，general plugin0.2.0-beta.5，SDK6.2.8，knowledge选择6.2，Editor6.2.4，emulator未安装；Knowledge依赖错误不用于本次查询，CLI有更新及项目未configure等警告。使用本地精确版本源码核对，未升级工具或SDK；设备ADB已独立验证可用。

待头显验证：连续三轮，观察末瓣释放→祝福淡入、点重玩→下一轮、第二/第三轮结束；应每轮出现一次`stage=reveal`，每次重玩一次`stage=erased`，日志revision=`buffer-gate-v2`。检查无旧文字/按钮闪现、600/450ms淡入及暂停恢复正常。若闪的是全画面或粒子而非完成面板，需要依据录制继续排查。

## 交付

`backups/completion-buffer-v2-2026-11-13/RoseTear-1.0.17.apk`

SHA-256 `60fdfcc6cc6ac516110ecb008750b8785b2bd942ba87110a8d0971ba3c1fc689`。

Git提交尝试被已有`.git/HEAD.lock`阻断（空文件，修改时间13:51:56）；HEAD仍为`c59e4fe`，本增量已暂存，未声称已提交，也未删除锁或更换Git目录。日志原始字节另存backup下`device-raw.log`，docs版本仅去除行尾空白。修复版runtime-audit也返回`TARGET_ROOT_TIMEOUT`，无第二轮动态验收证据。
