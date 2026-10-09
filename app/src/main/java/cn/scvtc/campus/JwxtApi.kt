package cn.scvtc.campus

import android.webkit.CookieManager
import cn.scvtc.campus.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class JwxtAuthenticationRequired : IllegalStateException("AUTH_REQUIRED：教务 Session 需要恢复；离线内容保留")

/** The WebView cookie store is shared by CAS navigation and native HTTP. Each
 * request asks the store for its own URL; a CAS cookie is never copied to JWGR. */
class JwxtCookieJar : CookieJar {
    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        CookieManager.getInstance().getCookie(url.toString()).orEmpty().split(';')
            .mapNotNull { Cookie.parse(url,it.trim()) }
    override fun saveFromResponse(url: HttpUrl,cookies: List<Cookie>) {
        val manager=CookieManager.getInstance()
        cookies.forEach { manager.setCookie(url.toString(),it.toString()) }
        manager.flush()
    }
}

data class JwxtStudent(val account:String,val fields:Map<String,String>)
data class JwxtSchedule(val extraction:Extraction,val recipe:QueryRecipe)

/** Endpoints and JSON parameters observed in the actual official student's SPA.
 * No hash route is sent as an HTTP API, and no HTML is accepted as student data. */
class JwxtApi(private val client:OkHttpClient=OkHttpClient.Builder()
    .cookieJar(JwxtCookieJar()).followRedirects(false).followSslRedirects(false)
    .connectTimeout(15,TimeUnit.SECONDS).readTimeout(30,TimeUnit.SECONDS).callTimeout(40,TimeUnit.SECONDS).build(),
    private val headers:(String)->Map<String,String> = {emptyMap()}) {
    companion object {
        const val BASE=School.ORIGIN+"/jwgr/api/"
        const val IDENTITY=BASE+"student/studentInfo/querySelf"
        const val TERM=BASE+"baseInfo/semester/selectCurrentXnXq"
        const val SCHEDULE=BASE+"arrange/CourseScheduleAllQuery/studentCourseSchedule"
        // The actual official range control accepts through week 30. The
        // calendar can extend it; student/semester/course values are never fixed.
        const val OBSERVED_MAX_WEEK=30
    }
    private suspend fun json(url:String,body:String?=null):String=withContext(Dispatchers.IO) {
        require(url in setOf(IDENTITY,TERM,SCHEDULE)) {"未核验的教务接口"}
        // Actual official interceptor: seconds + public protocol salt "lyedu".
        // Generate a new nonce for every request, including the recovery retry.
        val timestamp=(System.currentTimeMillis()/1000).toString()
        val csrf=java.security.MessageDigest.getInstance("MD5").digest((timestamp+"lyedu").toByteArray())
            .joinToString(""){"%02x".format(it.toInt() and 255)}
        val request=Request.Builder().url(url+"?_t="+timestamp).header("Referer",School.ORIGIN+"/jwgr/")
            .header("X-Requested-With","XMLHttpRequest")
        headers(url).forEach{(key,value)->
            if(key.lowercase() in setOf("permission","authorization","x-auth-token","token") && value.length<=8192)
                request.header(key,value)
        }
        request.header("csrfToken",csrf).header("Accept","application/json, text/plain, */*")
        if(body!=null)request.header("Origin",School.ORIGIN).post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
        val call=client.newCall(request.build())
        val cancellation=currentCoroutineContext().job.invokeOnCompletion { if(it is CancellationException)call.cancel() }
        try { call.execute().use { response ->
            if(response.code in setOf(401,403) || response.code in 300..399)throw JwxtAuthenticationRequired()
                        val text=response.body?.byteStream()?.use { input ->
                val output=java.io.ByteArrayOutputStream();val block=ByteArray(8192)
                while(true){currentCoroutineContext().ensureActive();val count=input.read(block);if(count<0)break;check(output.size()+count<=3_000_000){"教务响应超过读取上限"};output.write(block,0,count)}
                output.toString("UTF-8")
            }.orEmpty()
            if(text.trimStart().startsWith("<")){
                if(response.isSuccessful)throw JwxtAuthenticationRequired()
                error("教务接口 HTTP ${response.code}；原数据保留")
            }
            val root=runCatching {Json.parseToJsonElement(text).jsonObject}.getOrElse {error("PAGE_CHANGED：教务返回非 JSON 数据；原内容保留")}
            val code=root["code"]?.jsonPrimitive?.content
            if(code in setOf("401","403","51000010","53000010"))throw JwxtAuthenticationRequired()
            check(response.isSuccessful) {"教务接口 HTTP ${response.code}；原数据保留"}
            check(code in setOf("200","0") && root["success"]?.jsonPrimitive?.booleanOrNull!=false) {"教务业务响应未成功；原数据保留"}
            text
        }} finally {cancellation.dispose()}
    }
    suspend fun student(expected:String):JwxtStudent {
        val body=json(IDENTITY,"{}")
        val account=OfficialIdentity.account(body)
        check(account.isNotBlank()) {"学生身份 API 未返回唯一有效学生；原数据保留"}
        check(expected.isBlank() || account==expected) {"ACCOUNT_MISMATCH：官方账号与当前缓存账号不同"}
        val rows=Json.parseToJsonElement(body).jsonObject["data"]!!.jsonObject["rows"]!!.jsonArray
        val fields=rows.single().jsonObject.mapNotNull { (key,value) ->
            (value as? JsonPrimitive)?.contentOrNull?.takeUnless { Regex("(?i)password|pwd|token|cookie|authorization|csrf|session").containsMatchIn(key) }?.let { key to it }
        }.toMap()
        return JwxtStudent(account,fields)
    }
    suspend fun calendar():Pair<OfficialTerm,List<Int>> {
        val body=json(TERM)
        val term=OfficialSemester.parse(body)?:error("正式学期 API 结构已变化；原课表保留")
        val end=java.time.LocalDate.parse(Json.parseToJsonElement(body).jsonObject["data"]!!.jsonObject["jsrq"]!!.jsonPrimitive.content)
        val last=(java.time.temporal.ChronoUnit.WEEKS.between(term.firstMonday,end)+1).toInt()
        val maximum=maxOf(OBSERVED_MAX_WEEK,last)
        require(maximum in 1..100)
        return term to (1..maximum).toList()
    }
    suspend fun schedule(account:String,term:String,weeks:List<Int>):JwxtSchedule {
        require(weeks.isNotEmpty() && weeks==weeks.distinct().sorted() && weeks.all {it in 1..100})
        val body=buildJsonObject {
            put("semester",term);put("studentId",account);put("querySource","single");put("oddOrDouble",1)
            put("startWeek",weeks.first().toString());put("stopWeek",weeks.last())
            put("weeks",JsonArray(weeks.map(::JsonPrimitive)))
        }.toString()
        val raw=ScvtcParser.api(json(SCHEDULE,body),SCHEDULE,"schedule",account,term,weeks)
        require(raw.warnings.isEmpty() && (raw.meetings.isNotEmpty() || raw.emptyConfirmed)) {"课表 JSON 未完整识别；原课程保留"}
        // Exactly these weeks were requested with a successful JSON response.
        // The existing write guard still rejects an unproved empty full term.
        val data=raw.copy(full=true,coverageWeeks=weeks,meetings=raw.meetings.mapNotNull { m -> m.copy(weeks=m.weeks.filter(weeks::contains)).takeIf {it.weeks.isNotEmpty()} })
        return JwxtSchedule(data,QueryRecipe(SCHEDULE,"POST",body,mapOf("Content-Type" to "application/json","X-Requested-With" to "XMLHttpRequest"),weeks))
    }
}
