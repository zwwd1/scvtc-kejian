# 川职·课间

四川职业技术学院非官方 Android 应用，由 **zwwd1** 维护。1.0.0 是本项目第一版，当前维护版为 **1.2.1**。课间基于 SleepDown 完整源码重构，保留 Miuix 组件、课程与设置双入口底栏、日周课表、编辑、小组件和完整动效。

## 1.2.1

成绩与学分使用原生分栏、学期选择、搜索、排序和可展开卡片，补充加权绩点、确认所得学分及学校毕业要求。刷新由应用任务继续执行，离开成绩页后仍可完成；查询不修改手工课表。

[具体操作、修改位置与待用户验收事项](docs/UI_FLOWS_20261010.md)。保留现有 Miuix、壁纸和完整玻璃动效。本轮不连接手机，不执行功能或性能测试；必要构建结果见验证报告。

## 1.2.0

修复检查更新失败，优先读取本项目 GitHub 更新清单。使用原创川职月光壁纸和小澄五种状态，统一应用名称、更新和反馈渠道。保留原版玻璃与流畅动效，不按机型自动降低效果。

[按本次 IMG-01～IMG-17 的修改与验收](docs/OPTIMIZATION_20261010.md) · [原创素材](docs/BRANDING_20261010.md)。本轮不连接手机、不执行功能或性能测试，编译和签名不能代替实际体验。历史验收保留在对应日期报告中。

## 下载

- [1.2.1 原签名 APK](https://github.com/zwwd1/scvtc-kejian/releases/download/v1.2.1/scvtc-kejian-1.2.1.apk) · [SHA-256](https://github.com/zwwd1/scvtc-kejian/releases/download/v1.2.1/scvtc-kejian-1.2.1.apk.sha256)
- [1.2.1 发布页](https://github.com/zwwd1/scvtc-kejian/releases/tag/v1.2.1) · [对应源码](https://github.com/zwwd1/scvtc-kejian/archive/refs/tags/v1.2.1.zip)
- [构建与签名对应关系](BUILD_PROVENANCE.json) · [验证范围](VERIFICATION.md)

最低 Android 13。已有安装请直接覆盖，保留应用数据；不要先卸载。自行更换签名的构建不能覆盖本项目原签名 APK。

## 使用与原理

首次在官方 CAS 完成登录，App 继续建立教务 Session，并用需要学生身份的真实 JSON API 核验。成功后加密保存认证，再次启动检查 Session；过期时恢复 SSO 或使用本机凭据重建认证，再继续同步。登录 URL 到达主页不能单独证明成功。学校密码变更或额外验证仍需本人处理，失败保留最后成功数据。

| 需要什么 | 入口 |
| --- | --- |
| 安装、登录和日常操作 | [使用方法](docs/SCVTC_USAGE.md) |
| 认证、真实接口、缓存和编译 | [构建原理](docs/SCVTC_ARCHITECTURE.md) |
| 本机加密、网络和数据删除 | [隐私说明](docs/SCVTC_PRIVACY.md) |
| 问题反馈 | [本项目 Issues](https://github.com/zwwd1/scvtc-kejian/issues) |
| 更多校园功能 | [川职·知学](https://github.com/zwwd1/scvtc-zhixue) |

成绩与学分使用学校真实 JSON，保留实际学期和完整分页；毕业要求与成绩所得分开，未知字段不作为假零值。学校接口未核验的功能使用明确的官网入口，不编造通知、成绩或课表。

## 数据与构建

学校凭据由 Android Keystore 与 AES-256-GCM 保护，服务查询缓存按账号、学期和模块加密。普通课表 Room 数据依靠应用沙箱与设备存储加密，不宣称 SQLCipher 全库加密。**云同步与匿名统计暂停**。系统自动备份关闭；个人导出由用户主动操作。

原生界面位于 app/src/main/java/com/xiaomanjun/sleepdownschedule；学校认证与接口位于 cn/scvtc/campus，ScvtcNativeBridge 接入原 Room 和编辑流程。使用 JDK 21、Android SDK 37.0 和项目 Gradle Wrapper 构建。原签名配置通过仓库外环境变量提供；私钥、密码、个人截图和原始学校响应不进入源码。

发布工作流只重组已签名的本机 APK，核对 SHA-256、包名、版本、原证书和嵌入的源码提交，不接触 Android 私钥。正式 Release 标签指向 APK 对应源码，更新清单在发布文件可用后更新。

## 致谢与许可

应用基底：[SleepDown-Schedule](https://github.com/xiaomanjun233/SleepDown-Schedule)。保留 [署名非商业、源码可见许可](LICENSE.md) 及 [第三方声明](THIRD_PARTY_NOTICES.md)；这不是 OSI 定义的开源许可。Miuix、AndroidLiquidGlass 和适配资源沿用各自许可。应用中的维护、更新与反馈属于本项目，依法必要的原作者信息仅保留在致谢与许可中。本项目不代表学校或原项目官方。
