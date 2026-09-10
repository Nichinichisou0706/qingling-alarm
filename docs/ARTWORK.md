# 角色与图标来源

按用户指定，v1.1.0 的角色插画和应用图标采用 [Whom001x/-](https://github.com/Whom001x/-) 仓库的 `generated/base.png`。

参考提交：`783f1fbd2bc30d6b97cb05747a22ea95c383ebdc`。

处理仅包括去除洋红背景、缩放、裁剪角色头像和添加青绿色背景。脚本为 `tools/prepare-art.ps1`，输出为 `app/src/main/res/drawable-nodpi/mascot.png` 和 `character_icon.png`。

v1.2.0 增加五个时段角色状态：早晨使用 `waving.png`，午间使用 `idle.png`，下午使用 `review-repair.png`，晚间使用 `waiting.png`，深夜使用 `base.png`。脚本 `tools/prepare-period-art.ps1` 对每个原图做洋红去底、裁出最宽的一帧并统一放到 420×686 透明画布，输出 `mascot_morning.png`、`mascot_noon.png`、`mascot_afternoon.png`、`mascot_evening.png`、`mascot_night.png`。

素材权利归原作者所有；本项目不为原仓库素材重新授予许可。插画中的标记属于原素材，不表示本应用由相关品牌出品或获得背书。
