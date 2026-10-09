package cn.scvtc.campus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.scvtc.campus.core.CreditSummary
import com.xiaomanjun.sleepdownschedule.CourseScheduleApp
import com.xiaomanjun.sleepdownschedule.CourseScheduleTheme
import com.xiaomanjun.sleepdownschedule.app.ui.DetailActivityScaffold
import com.xiaomanjun.sleepdownschedule.app.ui.detailContentTopPadding
import com.xiaomanjun.sleepdownschedule.feature.schedule.autorefresh.AutoRefreshScheduleStore
import com.xiaomanjun.sleepdownschedule.feature.settings.SettingsActionButton
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity
import kotlinx.coroutines.*
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.basic.Card

/** Uses SleepDown's theme and detail scaffold while keeping its two-entry dock. */
class AcademicActivity:ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState);enableEdgeToEdge()
        setContent {
            val app=application as CourseScheduleApp
            val profile by AutoRefreshScheduleStore.observe(app).collectAsState()
            val account=profile?.takeIf{it.schoolId==ScvtcNativeBridge.SCHOOL}?.username.orEmpty()
            var config by remember{mutableStateOf(ScheduleConfigEntity(totalWeeks=20,currentWeek=1,notificationLeadMinutes=10))}
            var snapshot by remember(account){mutableStateOf<AcademicSnapshot?>(null)}
            var selectedTerm by rememberSaveable(account){mutableStateOf("all")}
            var showTerms by rememberSaveable{mutableStateOf(false)}
            var query by rememberSaveable{mutableStateOf("")}
            val state by AcademicRepository.state.collectAsState()
            val scope=rememberCoroutineScope()
            LaunchedEffect(account) {
                config=app.repository.activeSnapshot().config
                if(account.isNotBlank()) {
                    snapshot=withContext(Dispatchers.IO){AcademicRepository.cached(app,account)}
                    if(snapshot==null){AcademicRepository.refresh(app,account);snapshot=withContext(Dispatchers.IO){AcademicRepository.cached(app,account)}}
                }
            }
            CourseScheduleTheme(config=config) {
                DetailActivityScaffold("成绩与学分",config,onBack={finish()},compactTopBar=true){backdrop->
                    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(start=16.dp,end=16.dp,top=detailContentTopPadding()+12.dp,bottom=40.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        if(account.isBlank())item {
                            Text("连接一次学校账号后，可查看真实成绩与学分，并离线保存记录。")
                            SettingsActionButton("连接学校教务",backdrop,onClick={startActivity(Intent(this@AcademicActivity,ScvtcLoginActivity::class.java))})
                        } else {
                            item {
                                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                    val summary=CreditSummary.from(snapshot?.grades.orEmpty())
                                    Text("学校毕业学分要求：${snapshot?.requiredCredits?:"尚未读取"}",fontWeight=FontWeight.Medium)
                                    Text(if(snapshot==null)"成绩所得学分：尚未读取" else "成绩中已确认获得：${summary.display} 学分 · ${summary.courses} 门课程去重")
                                    Text(if(summary.uncounted>0)"${summary.uncounted} 条记录缺少课程代码或获得学分，未计入。" else "按课程代码合并重修记录，取官方获得学分的最大值。")
                                    Text("此项为成绩记录汇总，不能代替学校毕业资格审核。",style=MaterialTheme.typography.bodySmall)
                                    snapshot?.let{Text("上次成功：${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it.fetchedAt))}",style=MaterialTheme.typography.bodySmall)}
                                }}
                            }
                            item {
                                if(state.account==account)Text(state.message,style=MaterialTheme.typography.bodySmall)
                                SettingsActionButton(if(state.busy)"正在读取…" else "刷新成绩与学分",backdrop,onClick={
                                    if(!state.busy)scope.launch{AcademicRepository.refresh(app,account);snapshot=withContext(Dispatchers.IO){AcademicRepository.cached(app,account)}}
                                })
                                ArrowPreference(title=if(selectedTerm=="all")"全部学期" else selectedTerm,summary="来自学校实际学期列表",onClick={showTerms=!showTerms})
                            }
                            if(showTerms)items(listOf("all")+snapshot?.terms.orEmpty()){term->
                                ArrowPreference(title=if(term=="all")"全部学期" else term,onClick={selectedTerm=term;showTerms=false})
                            }
                            item{TextField(value=query,onValueChange={query=it},label="搜索课程名称或代码",modifier=Modifier.fillMaxWidth())}
                            val records=snapshot?.grades.orEmpty().filter{(selectedTerm=="all"||it.fields["学期"]==selectedTerm) && (it.title.contains(query)||it.fields["课程代码"].orEmpty().contains(query))}
                            if(records.isEmpty())item{Text(if(snapshot==null)"尚无已验证记录" else "该学期或搜索条件下没有成绩记录")}
                            items(records){record->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                                Text(record.title,fontWeight=FontWeight.Medium)
                                record.fields.filterKeys{it!="课程名称"}.forEach{(label,value)->Text("$label：$value",style=MaterialTheme.typography.bodyMedium)}
                            }}}
                        }
                    }
                }
            }
        }
    }
}
