# 川职·课间 1.2.0 验证记录

日期：2026-10-10。本轮重新构建 APK，沿用原签名；源码标签指向 APK 内嵌的构建提交。之后追加的发布记录与更新清单不改变生产代码。

| 项目 | 结果 |
| --- | --- |
| APK | `scvtc-kejian-1.2.0.apk`，19,903,356 字节 |
| 包名 / 版本码 | `cn.scvtc.campus.preview` / `13` |
| APK SHA-256 | `a7440f1bbbc293b4caf6d5b0a9aed4da4f8404f848dcdcbaca23f7a8cc9423d6` |
| 原签名证书 SHA-256 | `5cab58b4d47d14e66143be594d3a4eebb5624be7e8f6a15354126bdab47343b3` |
| 构建提交 | [c03dd87ebe4e8dbf77faa3538e72dd3c54f98b2c](https://github.com/zwwd1/scvtc-kejian/commit/c03dd87ebe4e8dbf77faa3538e72dd3c54f98b2c) |
| 编译、R8、资源处理、lintVital、原签名打包 | 通过 |
| APK 完整性、ZIP 16 KiB 对齐、源资产、赞赏码原图、R8 mapping | 通过 |
| 原生库 | 4 个；没有原生库时 ELF 对齐不适用，有原生库时逐个验证 LOAD 对齐 |
| 用户账号及密码明文扫描 | 解压条目 UTF-8 / UTF-16LE 扫描未检出；不是完整安全审计 |
| 源码 ZIP 与发布构建树 | 1909 个文件逐个 Git blob 校验一致；不含历史 APK 或私钥 |

没有连接或控制手机，没有安装、运行自动化功能测试、测量帧率或调用实际 AI 服务。本轮全量 Lint 没有完成，不能写为通过；正式打包所需的 lintVital 与上述产物检查已完成。旧版初次全量检查的 19 项问题在源码中修正，未重新跑全量；新版额外分析停在工具的类型解析后结束。

设置页、升级后保留数据、两种外观、壁纸一致性、待机状态、底栏参数、成绩页动画、检查更新与下载、自然 Session 过期恢复、不同机型和 Android 17 仍需用户实际验收。AI 需配置自己的 HTTPS 服务及密钥。云服务继续暂停，既有账号、课程、手动编辑和自选壁纸不清除。

[逐图修改与待验收项](docs/OPTIMIZATION_20261010.md) · [使用方法](docs/SCVTC_USAGE.md) · [隐私与加密](docs/SCVTC_PRIVACY.md)。1.0.0 的记录在 releases/1.0.0-verification.md，不能代替本轮验证。
