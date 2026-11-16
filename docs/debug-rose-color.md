# ADB 指定后续花色

版本 1.0.20 / code21。先打开调试版应用，再执行：

```sh
adb shell am broadcast -a com.example.flowertears.p18ab52cf.DEBUG_ROSE_COLOR -p com.example.flowertears.p18ab52cf --ei color 8
```

把最后的数字改为目标值即可：

| 数值 | 花色 |
| --- | --- |
| -1 | 随机 |
| 0 | 红 |
| 1 | 粉 |
| 2 | 浅粉 |
| 3 | 白 |
| 4 | 蓝色妖姬 |
| 5 | 黄 |
| 6 | 橙 |
| 7 | 淡紫 |
| 8 | 彩色 |

从下一次新建花朵起持续生效，直到设置其他值或应用重新启动。不会替换当前花朵；已经开始生成的花朵也不变。错误重试沿用失败轮的配置，覆盖值等到下一次真正新轮再用。指定色绕过最近三色避让；-1沿用原来的均匀抽样和避让规则。花瓣数量、配色布局和结局不被命令修改。

每个应用进程的设置初始为-1，新启动的LaunchActivity也在创建场景前重置为-1（配置恢复不重置）。设置只存在内存，不写偏好或系统属性；仅唤回同一个已有界面不算重新启动。需要明确重置可直接发送-1，或force-stop后重新打开。

校验范围来自RoseLayout.COLOR_COUNT，因此未来添加颜色时继续使用下一个编号。成功返回result=0及selectedColor；缺失、非整数或超范围返回result=1，旧值保留。未打开应用/非debug包时不会注册接收器，不能把没有selectedColor回执当成设置成功。接收器使用RECEIVER_EXPORTED且要求发送端DUMP权限，可供ADB shell使用。

验证：139主机测试、开发/仪器测试APK构建和实验API检查通过；SpatialUI 0错误/1既有大字号提示；A11Y源码审计0错误。头显安装并确认1.0.20/code21。真实ADB回执验证8保持、非法9/字符串拒绝且保留8、-1和0切换、8后停止重启恢复-1；详见debug-rose-color-device.json。停止重启测试结束后设置为默认随机。启动后crash buffer最新记录仍为11月13日，未见本次新记录。本次runtime-audit返回TARGET_ROOT_TIMEOUT，未发现可见应用根节点；连续指定颜色的新轮渲染尚待头显可见场景验证；其采样/保持/重试逻辑由主机测试验证。

APK：`backups/debug-rose-color-v1-2026-11-16/Petaluck-1.0.20.apk`
SHA-256：`9a9e66b4053fbc7fe09f3ba4726221c574b4e18a90419af31fa90bdfe1466bfb`
