# 川职·课间 1.2.1 验证记录

日期：2026-10-10。本轮重新构建 APK，沿用原签名；构建提交已在自己的仓库分支公开，与 APK 内嵌提交一致。之后追加的发布记录与更新清单不改变生产代码。

| 项目 | 结果 |
| --- | --- |
| APK | `scvtc-kejian-1.2.1.apk`，19,936,124 字节 |
| 包名 / 版本码 | `cn.scvtc.campus.preview` / `14` |
| APK SHA-256 | `bad92bf69587614ed7c14baaf109bd6c009a778396cbbb06d8270bcb9b477549` |
| 原签名证书 SHA-256 | `5cab58b4d47d14e66143be594d3a4eebb5624be7e8f6a15354126bdab47343b3` |
| 构建提交 | [4cf60ecadb652bf223605081d83f6de37c41522f](https://github.com/zwwd1/scvtc-kejian/commit/4cf60ecadb652bf223605081d83f6de37c41522f) |
| 编译、R8、资源处理、lintVital、原签名打包 | 通过 |
| APK 完整性、ZIP 16 KiB 对齐、源资产、赞赏码原图、R8 mapping | 通过 |
| 原生库 | 4 个；没有原生库时 ELF 对齐不适用，有原生库时逐个验证 LOAD 对齐 |
| 用户账号及密码明文扫描 | 解压条目 UTF-8 / UTF-16LE 扫描未检出；不是完整安全审计 |
| 源码 ZIP 与发布构建树 | 1912 个文件逐个 Git blob 校验一致；不含历史 APK 或私钥 |

没有连接或控制手机，没有安装、运行自动化功能测试、测量帧率或调用实际 AI 服务。本轮未运行全量 Lint，不能写为通过；正式打包所需的 lintVital 与上述产物检查已完成，没有用历史检查替代本次结果。

设置页、升级后保留数据、两种外观、壁纸一致性、待机状态、底栏参数、成绩页动画、检查更新与下载、自然 Session 过期恢复、不同机型和 Android 17 仍需用户实际验收。AI 需配置自己的 HTTPS 服务及密钥。云服务继续暂停，既有账号、课程、手动编辑和自选壁纸不清除。

[本轮交互修改与使用步骤](docs/UI_FLOWS_20261010.md) · [此前逐图修改](docs/OPTIMIZATION_20261010.md) · [使用方法](docs/SCVTC_USAGE.md) · [隐私与加密](docs/SCVTC_PRIVACY.md)。历史验证不能代替本轮验证。
