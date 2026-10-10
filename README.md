# 川职·课间

给四川职业技术学院同学用的课表工具。打开就能看今天上什么课，也能切到整周课表；临时调课、手动编辑、小组件和上课提醒都保留。

课间以 SleepDown 为基底，由 [zwwd1](https://github.com/zwwd1) 维护。界面沿用 Miuix 和原有玻璃动效。学校没有参与本项目，教务数据以学校系统为准。

[下载最新版](https://github.com/zwwd1/scvtc-kejian/releases/latest) · [使用方法](docs/SCVTC_USAGE.md) · [反馈问题](https://github.com/zwwd1/scvtc-kejian/issues)

## 开始使用

1. 安装 APK。已经装过课间的话，直接覆盖安装，保留原来的数据。
2. 打开“连接川职教务”，填写学校统一认证的学号和密码，点击“登录并自动同步”。
3. 身份核验后，应用会在后台读取课表和成绩。平时直接打开课表即可，会话过期会自动尝试重新认证。
4. 在课表设置里核对学期、开学日期和节次时间。手动改过的课程会保留，学校同步不会把这些编辑直接覆盖。

学校临时要求验证码、二次认证，或你改了学校密码时，需要补充一次认证。断网或认证失败仍能看上次保存的课表和成绩。

## 成绩与学分

成绩按学期查看，可以搜索课程、按成绩或学分排序。点开课程卡片能看学校返回的详情，“需要留意”筛选用于查看未通过的成绩。

学分页把已经获得的学分和学校毕业要求分开展示。只有学校确实返回毕业要求时才显示进度；重修记录按课程代码合并，不把各学期所得重复相加。学分达到要求不代表学校已完成毕业审核。

## 隐私

密码和学校会话保存在本机，使用 Android Keystore 与 AES-GCM 加密。不会写进源码、安装包资源、普通日志或导出备份。成绩查询缓存按账号加密；日常课表存放在应用自己的 Room 数据库里，依靠 Android 应用沙箱和设备存储保护。

云服务和匿名统计已暂停。AI 需要自行配置服务，只有你启用课表授权时才会发送课程上下文。

[完整隐私说明](docs/SCVTC_PRIVACY.md)

## 源码与构建

学校登录在 `cn/scvtc/campus`，课表界面在 `com/xiaomanjun/sleepdownschedule`。CAS 负责认证，教务接口负责读取数据，Room 保存课表；网页不用于冒充原生课表数据。

构建使用 JDK 21、Android SDK 37.0 和 Gradle Wrapper：

```powershell
.\gradlew.bat :app:assembleGithubRelease
```

自行构建需要自己的签名。正式版私钥放在仓库之外，同一应用的官方更新一直沿用原签名。下载页里的 APK、源码标签和校验文件应对应同一个版本。

[构建原理](docs/SCVTC_ARCHITECTURE.md) · [这版改了什么](release-notes/v1.2.2.md) · [实际验证范围](VERIFICATION.md)

## 致谢

[SleepDown-Schedule](https://github.com/xiaomanjun233/SleepDown-Schedule) 提供应用基底；Miuix、AndroidLiquidGlass 和其他组件沿用各自许可。必要的版权和许可信息保留在 [LICENSE.md](LICENSE.md) 与 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

需要更多教务功能，可以看看另一个项目：[川职·知学](https://github.com/zwwd1/scvtc-zhixue)。
