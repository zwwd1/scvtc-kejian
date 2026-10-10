package cn.scvtc.campus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.scvtc.campus.core.CreditSummary
import cn.scvtc.campus.core.NativeRecord
import com.xiaomanjun.sleepdownschedule.CourseScheduleApp
import com.xiaomanjun.sleepdownschedule.CourseScheduleTheme
import com.xiaomanjun.sleepdownschedule.app.ui.DetailActivityScaffold
import com.xiaomanjun.sleepdownschedule.app.ui.detailContentTopPadding
import com.xiaomanjun.sleepdownschedule.feature.schedule.autorefresh.AutoRefreshScheduleStore
import com.xiaomanjun.sleepdownschedule.feature.settings.SettingsGroup
import com.xiaomanjun.sleepdownschedule.feature.settings.SettingsActionButton
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import java.math.BigDecimal

/** Real academic records, using SleepDown's existing theme and detail scaffold. */
class AcademicActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as CourseScheduleApp
            val profile by AutoRefreshScheduleStore.observe(app).collectAsState()
            val account = profile?.takeIf { it.schoolId == ScvtcNativeBridge.SCHOOL }?.username.orEmpty()
            var config by remember { mutableStateOf(ScheduleConfigEntity(totalWeeks = 20, currentWeek = 1, notificationLeadMinutes = 10)) }
            var snapshot by remember(account) { mutableStateOf<AcademicSnapshot?>(null) }
            var tab by rememberSaveable(account) { mutableIntStateOf(0) }
            var selectedTerm by rememberSaveable(account) { mutableStateOf("all") }
            var query by rememberSaveable(account) { mutableStateOf("") }
            var sort by rememberSaveable(account) { mutableIntStateOf(0) }
            val state by AcademicRepository.state.collectAsState()
            LaunchedEffect(account) {
                config = app.repository.activeSnapshot().config
                if (account.isNotBlank()) {
                    snapshot = withContext(Dispatchers.IO) { AcademicRepository.cached(app, account) }
                    if (snapshot == null) AcademicRepository.requestRefresh(app, account)
                }
            }
            LaunchedEffect(account, state.account, state.busy) {
                if (account.isNotBlank() && state.account == account && !state.busy)
                    snapshot = withContext(Dispatchers.IO) { AcademicRepository.cached(app, account) }
            }
            val terms = remember(snapshot, selectedTerm) {
                listOf("all") + (snapshot?.terms.orEmpty() + listOf(selectedTerm).filter { it != "all" }).distinct().sortedDescending()
            }
            val termRecords = remember(snapshot, selectedTerm) {
                snapshot?.grades.orEmpty().filter { selectedTerm == "all" || it.fields["学期"] == selectedTerm }
            }
            val visibleRecords = remember(termRecords, query, sort) {
                val search = query.trim()
                val matching = termRecords.withIndex().filter {
                    it.value.title.contains(search, true) || it.value.fields["课程代码"].orEmpty().contains(search, true)
                }
                when (sort) {
                    1 -> matching.sortedWith(compareByDescending<IndexedValue<NativeRecord>> { it.value.fields["成绩"]?.toBigDecimalOrNull() }.thenBy { it.value.title })
                    2 -> matching.sortedWith(compareByDescending<IndexedValue<NativeRecord>> { it.value.fields["学分"]?.toBigDecimalOrNull() }.thenBy { it.value.title })
                    else -> matching
                }
            }
            CourseScheduleTheme(config = config) {
                DetailActivityScaffold("成绩与学分", config, onBack = { finish() }, compactTopBar = true) { backdrop ->
                    LazyColumn(Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = detailContentTopPadding() + 12.dp,
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (account.isBlank()) item {
                            SettingsGroup(backdrop, config) {
                                Text("连接一次学校账号后，可查看真实成绩与学分，并离线保存记录。", Modifier.padding(16.dp))
                                SettingsActionButton("连接学校教务", backdrop, onClick = {
                                    startActivity(Intent(this@AcademicActivity, ScvtcLoginActivity::class.java))
                                }, modifier = Modifier.padding(16.dp).fillMaxWidth())
                            }
                        } else {
                            item {
                                SettingsGroup(backdrop, config) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(if (state.account == account && state.message.isNotBlank()) state.message
                                            else if (snapshot == null) "尚未取得学校记录" else "本机成绩已保存，可离线查看")
                                        snapshot?.let { Text("最近成功同步：" + java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it.fetchedAt)),
                                            style = MaterialTheme.typography.bodySmall) }
                                        SettingsActionButton(if (state.busy) "正在同步，请稍候" else "刷新成绩与学分", backdrop,
                                            onClick = { if (!state.busy) AcademicRepository.requestRefresh(app, account) }, modifier = Modifier.fillMaxWidth())
                                        if (state.account == account && state.authRequired) SettingsActionButton("补充学校认证", backdrop, onClick = {
                                            startActivity(Intent(this@AcademicActivity, ScvtcLoginActivity::class.java))
                                        }, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                            item { TabRow(listOf("成绩", "学分"), tab, { tab = it }) }
                            if (tab == 0) {
                                item {
                                    SettingsGroup(backdrop, config) {
                                        WindowDropdownPreference(items = terms.map { if (it == "all") "全部学期" else it },
                                            selectedIndex = terms.indexOf(selectedTerm).coerceAtLeast(0), title = "学期",
                                            onSelectedIndexChange = { selectedTerm = terms[it] })
                                        WindowDropdownPreference(items = listOf("学校顺序", "成绩从高到低", "学分从高到低"),
                                            selectedIndex = sort, title = "排序", onSelectedIndexChange = { sort = it })
                                    }
                                }
                                item {
                                    SettingsGroup(backdrop, config) {
                                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            AcademicStat("加权绩点", academicGpa(termRecords), Modifier.weight(1f))
                                            AcademicStat("确认学分", if (snapshot == null) "--" else CreditSummary.from(termRecords).display, Modifier.weight(1f))
                                            AcademicStat("成绩记录", if (snapshot == null) "--" else termRecords.size.toString(), Modifier.weight(1f))
                                        }
                                    }
                                }
                                item {
                                    TextField(query, { query = it }, label = "搜索课程名称或代码", modifier = Modifier.fillMaxWidth())
                                    Text("显示 ${visibleRecords.size} / ${termRecords.size} 条 · 点卡片查看详情", Modifier.padding(top = 8.dp),
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                if (visibleRecords.isEmpty()) item {
                                    SettingsGroup(backdrop, config) {
                                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(if (snapshot == null) "尚无已验证记录" else if (query.isNotBlank()) "没有匹配的课程" else "该学期暂无成绩")
                                            Text(if (query.isNotBlank()) "试试课程名称或代码，或清空搜索条件。" else
                                                "刷新未成功时仍保留原缓存；不会把失败当成学校没有成绩。", style = MaterialTheme.typography.bodySmall)
                                            if (query.isNotBlank()) SettingsActionButton("清空搜索", backdrop, onClick = { query = "" })
                                        }
                                    }
                                }
                                items(visibleRecords, key = { scvtcKey(it.value.title + it.value.fields.toString()) + ":" + it.index }) { entry ->
                                    val record = entry.value
                                    var expanded by rememberSaveable(record) { mutableStateOf(false) }
                                    SettingsGroup(backdrop, config) {
                                        Column(Modifier.fillMaxWidth().clickable(role = Role.Button,
                                            onClickLabel = if (expanded) "收起成绩详情" else "展开成绩详情") { expanded = !expanded }
                                            .heightIn(min = 48.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(record.title, fontWeight = FontWeight.Medium)
                                                    Text(listOfNotNull(record.fields["课程性质"], record.fields["学期"]).filter(String::isNotBlank).joinToString(" · "),
                                                        style = MaterialTheme.typography.bodySmall)
                                                }
                                                Text(record.fields["成绩"].orEmpty().ifBlank { "--" }, style = MaterialTheme.typography.headlineSmall,
                                                    color = MaterialTheme.colorScheme.primary)
                                            }
                                            Text("学分 ${record.fields["学分"].orEmpty().ifBlank { "--" }} · 绩点 ${record.fields["绩点"].orEmpty().ifBlank { "--" }} · ${if (expanded) "收起详情 ▴" else "查看详情 ▾"}",
                                                style = MaterialTheme.typography.bodyMedium)
                                            AnimatedVisibility(expanded) {
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    record.fields.filterKeys { it != "课程名称" }.forEach { (label, value) ->
                                                        Text("$label：$value", style = MaterialTheme.typography.bodyMedium)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                item {
                                    val summary = CreditSummary.from(snapshot?.grades.orEmpty())
                                    SettingsGroup(backdrop, config) {
                                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text("成绩中已确认获得", style = MaterialTheme.typography.titleMedium)
                                            Text(if (snapshot == null) "--" else "${summary.display} 学分", style = MaterialTheme.typography.headlineLarge,
                                                color = MaterialTheme.colorScheme.primary)
                                            Text("学校毕业学分要求：${snapshot?.requiredCredits?.ifBlank { "尚未读取" } ?: "尚未读取"}")
                                            snapshot?.takeIf { it.requiredCreditsFetchedAt > 0 }?.let {
                                                Text("毕业要求最近成功同步：" + java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it.requiredCreditsFetchedAt)),
                                                    style = MaterialTheme.typography.bodySmall)
                                            }
                                            val requirement = snapshot?.requiredCredits?.toBigDecimalOrNull()
                                            if (snapshot != null && requirement != null && requirement.signum() > 0)
                                                Text("距离学分要求还差 ${(requirement - summary.confirmed).max(BigDecimal.ZERO).stripTrailingZeros().toPlainString()} 学分")
                                            Text("${summary.courses} 门课程已按课程代码合并重修记录，取官方获得学分的最大值。")
                                            if (summary.uncounted > 0) Text("${summary.uncounted} 条记录缺少课程代码或获得学分，未计入汇总。")
                                            Text("仅比较学校学分要求和已确认所得，不代表毕业资格审核通过。", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                item {
                                    SettingsGroup(backdrop, config) {
                                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("各学期成绩所得", fontWeight = FontWeight.Medium)
                                            snapshot?.terms.orEmpty().filter { term -> snapshot!!.grades.any { it.fields["学期"] == term } }.forEach { term ->
                                                val records = snapshot!!.grades.filter { it.fields["学期"] == term }
                                                Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { selectedTerm = term; tab = 0 }
                                                    .heightIn(min = 48.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text(term, Modifier.weight(1f))
                                                    Text("${CreditSummary.from(records).display} 学分  ›")
                                                }
                                            }
                                            Text("学期数据独立汇总；跨学期重修不能把各学期所得直接相加。", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AcademicStat(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

private fun academicGpa(records: List<NativeRecord>): String {
    val pairs = records.mapNotNull {
        val credit = it.fields["学分"]?.toBigDecimalOrNull()?.takeIf { value -> value.signum() > 0 } ?: return@mapNotNull null
        val point = it.fields["绩点"]?.toBigDecimalOrNull()?.takeIf { value -> value.signum() >= 0 } ?: return@mapNotNull null
        credit to point
    }
    val weight = pairs.fold(BigDecimal.ZERO) { sum, pair -> sum + pair.first }
    return if (weight.signum() == 0) "--" else pairs.fold(BigDecimal.ZERO) { sum, pair -> sum + pair.first * pair.second }
        .divide(weight, 2, java.math.RoundingMode.HALF_UP).toPlainString()
}
