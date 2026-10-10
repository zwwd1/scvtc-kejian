# 川职·课间

课间是给四川职业技术学院同学用的课表应用。它以 SleepDown 源码为基础，沿用 Miuix 页面、日周课表和玻璃底栏，把川职的登录、课表、成绩与学分接进来。

日常使用很简单：打开看课，点课程看详情，需要时再刷新教务。课程编辑、调课、提醒、小组件和本地导入导出都保留。

[下载 APK](https://github.com/zwwd1/scvtc-kejian/releases/latest) · [反馈问题](https://github.com/zwwd1/scvtc-kejian/issues) · [更新记录](release-notes/v1.2.3.md)

## 课间和知学怎么选

| | 课间 | [知学](https://github.com/zwwd1/scvtc-zhixue) |
| --- | --- | --- |
| 日常入口 | 课表和设置 | 首页、课表、服务中心和设置 |
| 界面基础 | SleepDown、Miuix 与原有动效 | zhengfang-apk，统一接入玻璃组件 |
| 教务记录 | 课表、成绩与学分 | 课表、成绩、学分与官网服务入口 |
| 适合的用法 | 主要看课表，顺手查成绩 | 把课程和更多教务操作放在一起 |

两版都由 [zwwd1](https://github.com/zwwd1) 维护，都是非官方、开源的学生项目。学校记录以教务系统为准。

## 安装后怎么用

需要 Android 13 或以上版本。首次安装且没有数据时，会打开“连接川职教务”。填学校统一认证的学号和密码，点击“登录并自动同步”即可；不需要自己复制 Cookie。

核验身份后先进入课表，数据在后台继续读取。以后会话过期，应用会尝试重新认证并继续同步。学校确实要求验证码或二次认证时，再打开学校认证页完成。修改学校密码后，也需要更新本机保存的密码。

已经安装过课间，直接覆盖安装。课表、课程编辑和已保存记录继续保留。刷新失败也能看上次的数据，不会把离线课表清空。

在设置里进入当前课表详情，可以调整开学日期、教学周、节次时间、显示规则和课前提醒。成绩按学期查看、搜索和筛选；学分页分别展示已获学分和学校返回的毕业要求。

外观只有一套设置：开启液态玻璃时使用折射与跟随动效，关闭后改用高斯模糊。壁纸、底栏和卡片参数可以自行调整。AI 助手是可选功能，需要配置自己的模型服务。

[详细使用方法](docs/SCVTC_USAGE.md)

## 它是怎样实现的

学校认证放在 `cn/scvtc/campus`，课表界面和编辑功能保留在 `com/xiaomanjun/sleepdownschedule`，数据规则放在 `school-core`。

登录走学校 CAS 认证，再建立教务会话；只有学生身份接口确认账号一致，才保存连接。课表、成绩和学分从真实教务接口读取，课表写入 Room 后由原生页面展示。同步合并学校变更时保留本地编辑，不靠网页截图生成课程。

密码、会话和成绩缓存使用 Android Keystore 与 AES-GCM 按账号加密。课表数据库放在应用沙箱中。普通导出不含登录凭据，云服务和匿名统计保持暂停。AI 的课程授权默认关闭。

[构建原理](docs/SCVTC_ARCHITECTURE.md) · [隐私说明](docs/SCVTC_PRIVACY.md)

## 自行构建

使用 JDK 21、Android SDK 37.0 和仓库里的 Gradle Wrapper：

```powershell
.\gradlew.bat :app:assembleGithubRelease
```

签名配置通过仓库外的文件和环境变量提供，具体配置见构建说明。自行签名的 APK 与正式版签名不同，不能互相覆盖；本项目正式更新沿用第一版的包名和签名。

正式包、源码标签和 SHA-256 随 [GitHub Release](https://github.com/zwwd1/scvtc-kejian/releases) 一起发布。[验证报告](VERIFICATION.md) 会注明实际完成的检查，以及尚需实机确认的部分。

## 致谢项目

[SleepDown-Schedule](https://github.com/xiaomanjun233/SleepDown-Schedule)、Miuix 和 AndroidLiquidGlass 为项目提供了基础。源码和第三方组件的许可见 [LICENSE.md](LICENSE.md) 与 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
