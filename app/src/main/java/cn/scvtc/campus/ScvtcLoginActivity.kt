package cn.scvtc.campus

import android.os.Bundle
import android.net.Uri
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.xiaomanjun.sleepdownschedule.CourseScheduleApp
import com.xiaomanjun.sleepdownschedule.CourseScheduleTheme
import com.xiaomanjun.sleepdownschedule.app.ui.DetailActivityScaffold
import com.xiaomanjun.sleepdownschedule.app.ui.detailContentTopPadding
import com.xiaomanjun.sleepdownschedule.feature.settings.SettingsActionButton
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity
import com.xiaomanjun.sleepdownschedule.feature.schedule.autorefresh.AutoRefreshScheduleStore
import kotlinx.coroutines.*

/** The official page is used only to establish CAS/JWXT sessions. The app never
 * parses this document as timetable data and never ignores certificate errors. */
class ScvtcLoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScvtcNativeBridge.interactiveLogin = true
        enableEdgeToEdge()
        setContent {
            val app = application as CourseScheduleApp
            var config by remember { mutableStateOf(ScheduleConfigEntity(totalWeeks = 20, currentWeek = 1, notificationLeadMinutes = 10)) }
            var message by remember { mutableStateOf("请完成一次官方登录；验证身份后会自动读取课表。凭据仅在本机加密保存。") }
            var attempt by remember { mutableIntStateOf(0) }
            val expected = remember { AutoRefreshScheduleStore.load(app)?.username.orEmpty() }
            val memory = remember { OfficialLoginMemory(app) }
            val api = remember { JwxtApi(headers = { memory.apiHeaders(expected, it) }) }
            val web = remember {
                WebView(this).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    memory.install(this) { expected }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val u = request.url
                            return !(u.scheme == "https" && u.userInfo == null &&
                                ((u.host in setOf("cas.scvtc.edu.cn", "jwxt.scvtc.edu.cn") && u.port in setOf(-1, 443)) ||
                                (u.host == "www.shulin-soft.com" && u.port == 8267 && u.path == "/casLogin.html")))
                        }
                        override fun onPageFinished(view: WebView, url: String) {
                            memory.prepare(view)
                            CookieManager.getInstance().flush()
                            if (Uri.parse(url).host == "www.shulin-soft.com") {
                                view.loadUrl(CasAuthManager.JWXT_SSO_ENTRY); return
                            }
                            if (memory.recoveryNavigation(view, expected, url)) return
                            if (expected.isNotBlank() && Uri.parse(url).host == "cas.scvtc.edu.cn")
                                view.postDelayed({ memory.recover(view, expected) {} }, 1500)
                        }
                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                            if (request.isForMainFrame) message = "学校页面连接失败；已保存的课表不受影响。恢复网络后重试。"
                        }
                    }
                }
            }
            DisposableEffect(web) { onDispose { web.stopLoading(); web.destroy() } }
            LaunchedEffect(Unit) { config = app.repository.activeSnapshot().config }
            LaunchedEffect(attempt) {
                web.loadUrl(CasAuthManager.JWXT_SSO_ENTRY)
                val finished = withTimeoutOrNull(10 * 60_000) {
                    while (isActive) {
                        delay(2000)
                        val student = try { api.student(expected) } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
                        if (student != null) {
                            message = "身份已核验，正在通过官方接口同步课表…"
                            val result = ScvtcNativeBridge.sync(app, student.account, recover = false)
                            message = result.message
                            if (result.success) {
                                Toast.makeText(app, result.message, Toast.LENGTH_LONG).show()
                                setResult(RESULT_OK); finish(); return@withTimeoutOrNull true
                            }
                            return@withTimeoutOrNull false
                        }
                    }
                    false
                }
                if (finished == null) message = "登录等待已结束；可重试，离线课表已保留。"
            }
            CourseScheduleTheme(config = config) {
                DetailActivityScaffold("连接川职教务", config, onBack = { finish() }, compactTopBar = true) { backdrop ->
                    Column(Modifier.fillMaxSize().padding(top = detailContentTopPadding()).navigationBarsPadding()) {
                        Text(message, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
                        AndroidView(factory = { web }, modifier = Modifier.fillMaxWidth().weight(1f))
                        SettingsActionButton("重新连接", backdrop, onClick = { attempt++ })
                    }
                }
            }
        }
    }
    override fun onDestroy() {
        ScvtcNativeBridge.interactiveLogin = false
        super.onDestroy()
    }
}
