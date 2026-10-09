package com.xiaomanjun.sleepdownschedule.feature.settings

import com.xiaomanjun.sleepdownschedule.app.ui.*
import com.xiaomanjun.sleepdownschedule.*

import com.xiaomanjun.sleepdownschedule.core.remoteconfig.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import java.math.BigDecimal

private data class PrivacyPolicySection(val title: String, val body: String)

private val SleepDownPrivacyPolicySections = listOf(
    PrivacyPolicySection("一、本机与学校", "川职·课间由 zwwd1 维护，是基于 SleepDown 的非官方适配项目。登录仅访问学校 CAS 和教务系统；课表数据由已验证身份的官方 JSON 接口读取。"),
    PrivacyPolicySection("二、凭据加密", "账号密码、认证备份由 Android Keystore 管理的 AES-GCM 密钥在本机加密，身份核验成功后才正式保存。私钥不能随课表导出。WebView 保留学校 Cookie，App 每次以真实学生接口确认会话有效。"),
    PrivacyPolicySection("三、课表与系统备份", "课表存储于应用私有 Room 数据库，受 Android 沙箱和设备文件加密保护；并非 SQLCipher 全库加密。系统云备份和设备迁移备份已关闭。用户主动导出的课表含课程与地点，请自行保管。"),
    PrivacyPolicySection("四、云服务与可选联网", "本版本暂停云同步、上游云配置和安装统计，不把学校密码或课程上传给维护者。可选 AI、天气、更新和第三方适配器在主动使用或明确启用后连接对应服务；AI 上下文可能包含你选定的课程。"),
    PrivacyPolicySection("五、退出与故障", "退出教务连接会取消自动同步，移除本机保存的密码与 WebView 登录状态，保留离线课表。网络故障、过期会话或解析失败不会清空课表。学校要求补充验证时仍需本人操作。"),
    PrivacyPolicySection("六、权限与反馈", "通知、闹钟和后台任务用于课程提醒与同步；文件和照片由系统选择器授权。反馈请使用 github.com/zwwd1/scvtc-kejian/issues，只提交脱敏信息，不发送账号密码、Cookie 或原始抓包。")
)

@Composable
fun PrivacyPolicySettingsScreen(state: AppState, backdrop: Backdrop?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = detailContentTopPadding(), bottom = DockScrollPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "privacy-meta") {
            SettingsGroup(backdrop = backdrop, config = state.config, modifier = Modifier.fillMaxWidth()) {
                SettingsInfoRow(
                    "川职·课间隐私政策",
                    "版本 1.0\n更新日期：2026 年 10 月 8 日\n生效日期：2026 年 10 月 8 日\n\n请在使用前完整阅读。各项联网能力均不影响本机课表的基本查看与编辑。"
                )
            }
        }
        SleepDownPrivacyPolicySections.forEachIndexed { index, section ->
            item(key = "privacy-section-$index") {
                SettingsGroup(backdrop = backdrop, config = state.config, modifier = Modifier.fillMaxWidth()) {
                    SettingsInfoRow(section.title, section.body)
                }
            }
        }
    }
}

private fun RemoteDonationEntry.formattedAmount(): String {
    val number = BigDecimal.valueOf(amountCents, 2).toPlainString()
    return when (currency) {
        "CNY" -> "¥$number"
		"USD" -> "\$$number"
        "EUR" -> "€$number"
        else -> "$currency $number"
    }
}

@Composable
fun DonationThanksPanel(
	state: AppState,
	backdrop: Backdrop?,
	section: RemoteDonationSection
) {
	SettingsGroup(backdrop = backdrop, config = state.config, modifier = Modifier.fillMaxWidth()) {
		SettingsInfoRow(
			section.title.ifBlank { "捐赠致谢" },
			section.message.ifBlank { "感谢每一份支持。" }
		)
		SettingsDivider()
		Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
			Text("ID", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
			Text("捐赠金额", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
		}
		section.entries.filter(RemoteDonationEntry::enabled).forEach { item ->
			SettingsDivider()
			Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 15.dp)) {
				Text(item.supporterId, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
				Text(item.formattedAmount(), modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
			}
		}
		if (section.entries.none(RemoteDonationEntry::enabled)) {
			SettingsDivider()
			Text("名单已发布，暂时还没有公开条目。", modifier = Modifier.fillMaxWidth().padding(20.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
		}
    }
}
