package cn.scvtc.campus

import android.os.Bundle
import android.net.Uri
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.xiaomanjun.sleepdownschedule.CourseScheduleApp
import com.xiaomanjun.sleepdownschedule.CourseScheduleTheme
import com.xiaomanjun.sleepdownschedule.app.ui.DetailActivityScaffold
import com.xiaomanjun.sleepdownschedule.app.ui.detailContentTopPadding
import com.xiaomanjun.sleepdownschedule.feature.settings.SettingsActionButton
import com.xiaomanjun.sleepdownschedule.feature.settings.SettingsGroup
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity
import com.xiaomanjun.sleepdownschedule.feature.schedule.autorefresh.AutoRefreshScheduleStore
import kotlinx.coroutines.*
import top.yukonga.miuix.kmp.basic.TextField

class SchoolLoginForm : ViewModel() {
    var account by mutableStateOf("")
    var password by mutableStateOf("")
    override fun onCleared() { password="" }
}

class ScvtcLoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScvtcNativeBridge.interactiveLogin = true
        enableEdgeToEdge()
        val form = ViewModelProvider(this)[SchoolLoginForm::class.java]
        val app = application as CourseScheduleApp
        if(form.account.isBlank()) form.account=AutoRefreshScheduleStore.load(app)?.username.orEmpty()
        setContent {
            var config by remember { mutableStateOf(ScheduleConfigEntity(totalWeeks=20,currentWeek=1,notificationLeadMinutes=10)) }
            var awaiting by rememberSaveable { mutableStateOf(false) }
            var verification by rememberSaveable { mutableStateOf(false) }
            var localError by remember { mutableStateOf("") }
            val state by ScvtcNativeBridge.loginState.collectAsState()
            val callerScope=rememberCoroutineScope()
            LaunchedEffect(Unit) { config=app.repository.activeSnapshot().config }
            LaunchedEffect(state,awaiting) {
                if(awaiting && state.account==form.account && state.verified) {
                    form.password=""
                    setResult(RESULT_OK);finish()
                }
            }
            BackHandler(verification) { verification=false }
            CourseScheduleTheme(config=config) {
                DetailActivityScaffold(if(verification) "补充学校认证" else "连接川职教务",config,
                    onBack={if(verification) verification=false else finish()},compactTopBar=true) { backdrop ->
                    if(verification) {
                        SchoolVerification(form.account,
                            Modifier.fillMaxSize().padding(top=detailContentTopPadding()).navigationBarsPadding()) {
                            form.password="";setResult(RESULT_OK);finish()
                        }
                    } else Column(Modifier.fillMaxSize().padding(top=detailContentTopPadding()).imePadding()
                        .navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement=Arrangement.spacedBy(16.dp)) {
                        SettingsGroup(backdrop,config) {
                            Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                                Text("输入一次，后续自动同步")
                                Text("使用学校统一认证的学号和密码。身份核验成功后，应用会在后台读取真实课表；以后会话过期也会自动恢复。")
                                TextField(form.account,{form.account=it.trim()},label="学号",
                                    enabled=!state.busy,singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),
                                    modifier=Modifier.fillMaxWidth())
                                TextField(form.password,{form.password=it},label="学校密码",
                                    enabled=!state.busy,singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),
                                    visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
                                if(localError.isNotBlank()) Text(localError)
                                if(awaiting && state.account==form.account && state.message.isNotBlank()) Text(state.message)
                                if(!state.busy) SettingsActionButton("登录并自动同步",backdrop,onClick={
                                    if(!form.account.matches(Regex("[0-9]{6,20}")) || form.password.isEmpty()) {
                                        localError="请输入学号和密码"
                                    } else {
                                        val task=ScvtcNativeBridge.signIn(app,form.account,form.password)
                                        localError="";awaiting=true
                                        callerScope.launch {
                                            try { task.await() }
                                            catch(e:CancellationException){throw e}
                                            catch(_:Exception){ }
                                        }
                                    }
                                },modifier=Modifier.fillMaxWidth())
                            }
                        }
                        Text("密码只保存在本机的加密存储中，不会进入备份、日志或云端。")
                        if(!state.busy) SettingsActionButton("学校要求验证码？补充认证",backdrop,
                            onClick={
                                if(form.account.matches(Regex("[0-9]{6,20}"))) verification=true
                                else localError="请先填写学号，再打开补充认证"
                            },modifier=Modifier.fillMaxWidth(),monochrome=true)
                    }
                }
            }
        }
    }
    @Composable
    private fun SchoolVerification(account:String,modifier:Modifier,onVerified:()->Unit) {
        var message by remember { mutableStateOf("已填信息会保留；请完成学校要求的验证码或二次认证。") }
        val app=application as CourseScheduleApp
        val memory=remember { OfficialLoginMemory(app) }
        val api=remember(account) { JwxtApi(headers={memory.apiHeaders(account,it)}) }
        val web=remember(account) {
            WebView(this).apply {
                settings.javaScriptEnabled=true;settings.domStorageEnabled=true
                settings.allowFileAccess=false;settings.allowContentAccess=false
                settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this,true)
                memory.install(this){account}
                webViewClient=object:WebViewClient() {
                    override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean {
                        val u=request.url
                        return !(u.scheme=="https" && u.userInfo==null &&
                            ((u.host in setOf("cas.scvtc.edu.cn","jwxt.scvtc.edu.cn") && u.port in setOf(-1,443)) ||
                            (u.host=="www.shulin-soft.com" && u.port==8267 && u.path=="/casLogin.html")))
                    }
                    override fun onPageFinished(view:WebView,url:String) {
                        memory.prepare(view);CookieManager.getInstance().flush()
                        if(Uri.parse(url).host=="www.shulin-soft.com") { view.loadUrl(CasAuthManager.JWXT_SSO_ENTRY);return }
                        if(memory.recoveryNavigation(view,account,url)) return
                        if(account.isNotBlank() && Uri.parse(url).host=="cas.scvtc.edu.cn") memory.recover(view,account){}
                    }
                    override fun onReceivedError(view:WebView,request:WebResourceRequest,error:WebResourceError) {
                        if(request.isForMainFrame) message="学校页面连接失败；原课表保留，可稍后重试。"
                    }
                }
            }
        }
        DisposableEffect(web) { onDispose {web.stopLoading();web.destroy()} }
        LaunchedEffect(web) {
            web.loadUrl(CasAuthManager.PROVIDED_H5_ENTRY)
            withTimeoutOrNull(10*60_000) {
                while(isActive) {
                    delay(2000)
                    val student=try {api.student(account)}
                    catch(e:CancellationException){throw e}
                    catch(_:Exception){null}
                    if(student!=null) {
                        memory.confirmed(student.account,promoteEnrollment=true)
                        app.applicationScope.launch {
                            val result=ScvtcNativeBridge.sync(app,student.account,recover=false)
                            if(result.success) AcademicRepository.requestRefresh(app,student.account)
                        }
                        onVerified();return@withTimeoutOrNull
                    }
                }
            }
            message="补充认证等待已结束，离线记录仍可使用。"
        }
        Column(modifier) {
            Text(message,Modifier.fillMaxWidth().padding(16.dp))
            AndroidView(factory={web},modifier=Modifier.fillMaxWidth().weight(1f))
        }
    }
    override fun onDestroy() { ScvtcNativeBridge.interactiveLogin=false;super.onDestroy() }
}
