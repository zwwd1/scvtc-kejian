# 川职·课间

四川职业技术学院非官方课表应用，维护：**zwwd1**。本项目 `1.0.0` 为第一版。

**本项目基于 SleepDown课程表 v1.2.6 完整源码修改；原作者：xiaomanjun233；原项目：https://github.com/xiaomanjun233/SleepDown-Schedule。** 这是独立川职适配分支，不代表学校或 SleepDown 官方。原许可、版权和组件说明保留。

课间以课程为中心，沿用 SleepDown 的 Miuix 组件、双入口玻璃底栏、日周视图、渐进切周、课程编辑、弹窗和切页动效。川职认证和同步接入原生界面及 Room，不再以 OpenWakeUp 作为界面基底。

## 1.1.0 维护版

本轮新增真实成绩与学分、实时同步反馈与右上角液态玻璃刷新，修正重复导入产生的误报冲突和单周调整。保留 SleepDown 的原有双入口、组件和动效。两版图标按用户二次元参考重新生成，校园助手小澄提供微笑、侧目、眨眼、思考和点击回应。

[按图片逐项的修改与验收](docs/OPTIMIZATION_20261009.md) · [生图素材与提示词](docs/BRANDING_20261009.md)。[1.1.0 源码分支](https://github.com/zwwd1/scvtc-kejian/tree/codex/unified-optimization-20261009)。本轮 APK 在本机以原签名打包，含用户指定的赞赏原图；为避免公开支付资料，原图和这批 APK 不进入公开 Git。下方 1.0.0 为保留的历史第一版，不能当作本轮修复产物。

## 下载与验证

- [1.0.0 原签名 APK](https://raw.githubusercontent.com/zwwd1/scvtc-kejian/main/dist/scvtc-campus-old-1.0.0.apk) · [SHA-256](dist/scvtc-campus-old-1.0.0.apk.sha256)
- [完整源码](https://github.com/zwwd1/scvtc-kejian/archive/refs/heads/main.zip) · [验证记录](VERIFICATION.md) · [源码与 APK 对应关系](BUILD_PROVENANCE.json)
- 最低 Android 13；覆盖安装请保留应用数据。仓库源码不含原签名私钥，自行使用其他私钥打包不能覆盖此 APK。

[交互架构图](docs/architecture.html)：下载 HTML 后在浏览器打开，节点可跳转到固定源码提交。

## 使用入口

| 需要什么 | 说明 |
| --- | --- |
| 首次登录、看课与自动刷新 | [使用方法](docs/SCVTC_USAGE.md) |
| 基底、认证、接口与数据迁移 | [构建原理](docs/SCVTC_ARCHITECTURE.md) |
| 本机加密、联网与删除 | [隐私说明](docs/SCVTC_PRIVACY.md) |
| 自行编译与签名 | [源码构建](docs/SCVTC_ARCHITECTURE.md#构建) |
| 问题反馈 | [本项目 Issues](https://github.com/zwwd1/scvtc-kejian/issues) |
| 更多校园功能 | [川职·知学](https://github.com/zwwd1/scvtc-zhixue) |

## 工作方式

首次在官方 CAS 页面完成登录后，App 继续建立教务 Session，以真实学生身份 JSON 验证当前账号，再读取个人课表。密码和认证备份使用 Android Keystore 与 AES-256-GCM 在本机保存。之后优先复用会话，失效时自动尝试恢复并继续同步；学校要求补充认证时仍需本人完成。

课表按账号和学期保存，支持离线查看。同步失败保留课程，本地手动编辑与删除也保留。学校未提供的上下课时间不编造，需要提醒时可配置实际作息。

覆盖升级沿用 `cn.scvtc.campus.preview` 与原签名。旧课表从原数据库只读迁移，迁移完成标记和课程一起提交，源数据保留；加密登录的 Keystore 身份沿用。新安装、历史版本升级和真实 Session 过期恢复分别验证，不把其中一个通过称作全部通过。

## 隐私

**云同步、上游云配置和安装统计暂停。** 系统云备份与设备迁移备份关闭。密码、Cookie、个人抓包与签名私钥不进入 Git、APK 明文资源或普通导出。普通课程缓存由应用沙箱与 Android 设备加密保护，不称为 SQLCipher 全库加密。

可选 AI、天气、适配资源和项目更新有各自联网行为；主动导出可能包含课程、教师和地点，需作为个人文件保管。

## 源码构建

JDK 21、Android SDK Platform 37.0、Build Tools 37.0.0，使用项目 Gradle Wrapper。`third-party/miuix` 是带 SleepDown 补丁的 Miuix 0.9.3，`third-party/kyant-backdrop` 保留原版玻璃实现。版本、包名、签名和源码对应关系应随 APK 一起核对。

已完成本地发布构建、原签名与对齐检查；在 Android 13 真机上验证了保留数据覆盖升级、真实课表读取和应用重启后免输入同步。本轮相关学分、冲突和单周规则共 14 项检查通过。后台自然长期过期、Android 17 等边界见 [本轮验收](docs/OPTIMIZATION_20261009.md)，历史记录保留在 [VERIFICATION.md](VERIFICATION.md)。临时脚本、构建缓存、私人配置与诊断材料留在仓库外。

## 致谢项目与许可

- [SleepDown-Schedule · xiaomanjun233](https://github.com/xiaomanjun233/SleepDown-Schedule)：应用基底、UI、动效与设计系统。
- [Miuix](https://github.com/compose-miuix-ui/miuix)：界面组件，沿用基底补丁。
- [AndroidLiquidGlass · Kyant0](https://github.com/Kyant0/AndroidLiquidGlass)：玻璃渲染。
- [shiguang_warehouse](https://github.com/xingheyuzhuan/shiguang_warehouse)：基底保留的适配资源。

遵循 [SleepDown 署名非商业、源码可见许可 1.1](LICENSE.md)，不是 OSI 定义的开源许可。第三方组件按 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 中的各自许可使用。原项目说明保留在 [UPSTREAM_README.md](docs/UPSTREAM_README.md)。
