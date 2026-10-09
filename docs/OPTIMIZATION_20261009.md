# 2026-10-09 统一维护与验收

基线 main `cf025aea7c5dcc24daeea22168701747942575ee`。维护版 1.1.0 / versionCode 12 保留 `cn.scvtc.campus.preview`、原证书、Room、现有课表和设置；1.0.0 第一版产物保持原样。继续使用 SleepDown 1.2.6 的完整源码、Miuix 和双入口底栏。云服务继续暂停。

| 图片 / 任务 | 修改文件 | 验证结果 | 未解决或未验证 |
| --- | --- | --- | --- |
| IMG-01 小组件顶部 | `ExpandedScheduleWidgetProviders.kt`、四份 `widget_week_schedule*.xml` 与预览布局 | 标题按 Host 尺寸自动缩放、限制信息行、预留标题末端空间；打包检查通过 | Launcher 编辑模式蓝柄不能由 App 移除；各 Host 尺寸、旋转和放大字体尚未全部实测 |
| IMG-02、IMG-03 赞赏 | `DonateUi.kt`、`CampusDonationImage.kt`、`app/build.gradle.kts` | 私有构建输入使用原始 PNG；显示与保存均保持原码字节；最终 APK 检查原 PNG 一致性 | 微信实际扫码尚未完成，不把 PNG 一致等同扫码通过 |
| IMG-04 同步与查询 | `CampusSyncStatus.kt`、`ScvtcNativeBridge.kt`、`HomeScheduleUi.kt`、`ScheduleAppUi.kt` | 真实认证、读取、事务保存与成功时间；右上角复用液态按钮刷新；阶段中的按钮不能重复发起同步 | Android/OEM 后台执行时机与自然长期过期恢复没有全覆盖 |
| IMG-04 成绩与学分 | `JwxtApi.kt`、`JwxtServices.kt`、`AcademicRepository.kt`、`AcademicActivity.kt`、`CreditSummary.kt` | 真机已从真实 JSON 读取 39 条成绩；官方返回 27 个学期；按账号/学期 AES-GCM 缓存在原 Room 表；毕业要求与成绩所得分开显示 | 尚未进行多学生账号切换真机验收；不会推算毕业资格或虚构 GPA |
| IMG-07 壁纸、卡片和材质 | `GlassUi.kt`、`SharedBlurBackdrop.kt` | 按用户后续要求撤回本轮共享模糊采样调整，生产端 0.48、卡片缓冲 1.0 恢复原基线；原壁纸、裁剪、玻璃与手势保留 | 最终新角色待机版的性能另行记录；亮暗主题、所有参数组合尚未穷举 |
| IMG-04、IMG-07 图标与角色 | `CampusCompanion.kt`、`campus_icon_*.xml`、`drawable-nodpi/campus_*.png`、`BRANDING_20261009.md` | 按用户最新参考生成扁平二次元头像；小澄拥有微笑/侧目/眨眼待机、思考和点击回应；后台停循环，IO 解码、2 MiB 缓存；沿用显隐、缩放和位置键 | 最终新图标已覆盖安装，标题角色与刷新按钮可见；完整五状态循环、修复后设置页待实机确认 |
| 追加：设置闪退 | `AppIconMode.kt` | 真机复现：设置页 painterResource 不支持 XML bitmap；应用内图标改为直接 PNG，桌面 Adaptive Icon 保留；未用 catch 隐藏问题 | 修复源码重新构建；用户已暂停手机调试，最终包的设置页待实机确认 |
| 追加：误报冲突与单周调整 | `CourseConflictResolution.kt`、`WeekScheduleUi.kt`、`HomeScheduleUi.kt` | 完全相同的课程安排在当前周视图去重，不删除数据库行；真实课程冲突仍保留；点击调整先确认；只改单次不改其他周。11 项冲突规则和 1 项单周范围测试通过 | 另一部手机版本未知，无法确认其本地记录与本机一致 |

IMG-05、IMG-06、IMG-08～IMG-17 的新版菜单、页脚、服务精简与官网修复见 [知学维护记录](https://github.com/zwwd1/scvtc-zhixue/blob/codex/unified-optimization-20261009/docs/OPTIMIZATION_20261009.md)。

## 设备与实际数据

Redmi K40 / Android 13（API 33）。已保留数据覆盖安装阶段版、读取完整课表与真实成绩、重启后复用认证和已有壁纸。排查期间曾误触原版的一键移动；已通过单次编辑恢复该周安排，并确认其他周原有安排保留。修复增加确认，不会再仅点冲突标记就写入调整。另一台手机、Android 17 与 OEM 组合没有实际验收。

本轮没有再次手工输入学校账号密码。可见官方 CAS 页面曾自动恢复，并继续得到真实学生身份与课表；这不能替代全部后台自然过期场景的验证。凭据、成绩原文、Cookie、抓包与屏幕证据只留本机；公开文档仅记录脱敏数量与结果。

## 定向检查

- `CourseConflictResolutionTest`：11 项通过。
- `CourseWeekScopeTest`：1 项通过。
- `CreditSummaryTest`：2 项通过。
- 以上是本轮相关检查，不代表全项目所有测试、所有学校和所有升级来源通过。
- Release 打包使用 JDK 21、单 worker、R8 与资源压缩；正式 APK 仍核对原证书、包名、版本、ZIP/ELF 16 KiB 对齐及 R8 mapping。

## 帧时间

相同教学周、壁纸、60 Hz、字体 1.0，2 次预热往返和 6 次测量往返。视觉追加前的阶段包：1.0.0 的 p50/p95 为 73/97 ms，阶段 1.1.0 为 44/57 ms；卡顿率 100% → 90.03%。这些数值来自系统 gfxinfo，不代表达到 60 fps，也不是最终新增待机资源后的性能结论。用户反馈希望恢复原效果，本轮采样优化已撤回，这些阶段测量不能作为最终包收益。用户随后允许手动可改的参数，沿用已有首页玻璃模糊与课程材质设置，默认保留原版效果；不按机型自动降级。

最终 APK 哈希与源码对应信息见本机交付 `DELIVERY_20261009.md`；公开仓库不包含用户支付图、签名私钥和新 APK。

最终闪退修复和恢复原效果后的构建结果，与此前阶段包的设备结果分别记录。已确认测试机为 Redmi K40 / M2012K11AC / Android 13（API 33）；另一台手机版本仍未知。用户已暂停手机调试，最终包没有继续实机验收。
