# 终章修订 v2

## 祝福闪帧

完成面板第一次使用后保持 native owner 启用，非完成期 opacity=0 且 Compose 子内容为空，避免后续轮次重新激活旧合成缓冲。每次实际 draw 显式 BlendMode.Clear，再按原有提交门控绘制；保留 reveal/erase serial 与回调过期检查。主机测试覆盖三轮空白保持和旧回调拒绝。硬件合成与输入穿透仍需设备复测。
