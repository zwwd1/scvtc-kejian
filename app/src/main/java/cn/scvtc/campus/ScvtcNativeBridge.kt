package cn.scvtc.campus

import android.content.Context
import androidx.room.withTransaction
import cn.scvtc.campus.core.Meeting
import com.xiaomanjun.sleepdownschedule.CourseScheduleApp
import com.xiaomanjun.sleepdownschedule.TodayCoursesWidgetProvider
import com.xiaomanjun.sleepdownschedule.model.*
import com.xiaomanjun.sleepdownschedule.feature.schedule.autorefresh.*
import com.xiaomanjun.sleepdownschedule.feature.reminder.NotificationScheduler
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.security.MessageDigest
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal fun scvtcKey(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }

internal fun Meeting.nativeCourse(scheduleId: Int) = CourseEntity(
    name = name, teacher = teacher.ifBlank { null }, location = room.ifBlank { null },
    weekday = day, periods = (startNode..endNode).toList(), weeks = weeks.distinct().sorted(),
    weekParity = WeekParity.ALL, note = null, scheduleId = scheduleId
)

/** CAS only authenticates. All imported course data comes from verified JSON APIs. */
internal object ScvtcNativeBridge {
    internal val coordinator = Mutex()
    private var foregroundAttempt = 0L
    @Volatile var interactiveLogin = false
    private var loginJob: Deferred<JwxtStudent>? = null
    val loginState = kotlinx.coroutines.flow.MutableStateFlow(NativeLoginState())
    @Synchronized
    fun signIn(app: CourseScheduleApp, account: String, password: String): Deferred<JwxtStudent> {
        loginJob?.takeIf { !it.isCompleted }?.let { return it }
        require(account.matches(Regex("[0-9]{6,20}")) && password.length in 1..512) { "请输入学号和密码" }
        loginState.value = NativeLoginState(account,busy=true,message="正在连接学校并核验学生身份…")
        val job = app.applicationScope.async(start = CoroutineStart.LAZY) {
            try {
                val student = coordinator.withLock {
                    val memory = OfficialLoginMemory(app)
                    val api = JwxtApi(headers = { memory.apiHeaders(account,it) })
                    CasAuthManager(app,api).signIn(account,password)
                }
                loginState.value = NativeLoginState(account,verified=true,message="学校身份已核验，正在后台读取课表")
                student
            } catch(e: CancellationException) {
                loginState.value = NativeLoginState(account,message="已停止本次登录，原数据保留"); throw e
            } catch(e: Exception) {
                val extra = e is JwxtAuthenticationRequired
                loginState.value = NativeLoginState(account,requiresVerification=extra,
                    message=if(extra) "学校暂时需要补充认证，已填信息会保留" else "这次登录未完成，请检查网络或稍后重试；原数据保留")
                throw e
            }
        }
        loginJob = job
        job.invokeOnCompletion { cause ->
            synchronized(this) { if(loginJob===job) loginJob=null }
            if(cause==null) app.applicationScope.launch {
                val result=sync(app,account)
                if(result.success) AcademicRepository.requestRefresh(app,account)
            }
        }
        job.start()
        return job
    }
    const val SCHOOL = "scvtc"
    fun profileKey(account: String, term: String) = "profile:" + scvtcKey("$account|$term")
    fun fingerprint(course: CourseEntity) = scvtcKey(course.copy(id = 0).toString())
    private fun schoolKey(course: CourseEntity) = scvtcKey(listOf(course.name, course.teacher,
        course.location, course.weekday, course.periods).joinToString("|"))

    fun foreground(app: CourseScheduleApp) {
        if (interactiveLogin) return
        val now = System.currentTimeMillis()
        if (now - foregroundAttempt < 5 * 60_000) return
        foregroundAttempt = now
        app.applicationScope.launch {
            val migration = runCatching { ScvtcLegacyMigration.run(app) }
            if (migration.isFailure) return@launch // Never synchronize over an incomplete migration.
            val profile = AutoRefreshScheduleStore.load(app)
            val account = profile?.takeIf { it.schoolId == SCHOOL }?.username
                ?: OfficialLoginMemory(app).savedAccounts().singleOrNull()
            if (account != null && profile?.automatic != false) sync(app, account)
        }
    }

    suspend fun sync(context: Context, expectedAccount: String, recover: Boolean = true): AutoRefreshOutcome = coordinator.withLock {
        val app = context.applicationContext as CourseScheduleApp
        val memory = OfficialLoginMemory(app)
        val old = AutoRefreshScheduleStore.load(app)
        CampusSyncStatus.begin(app,expectedAccount,previousSuccess=old?.lastRefreshAt?:0)
        try {
            ScvtcLegacyMigration.run(app)
            CampusSyncStatus.phase(SyncStage.AUTHENTICATING,"正在认证学校账号…")
            memory.restoreSession(expectedAccount)
            val api = JwxtApi(headers = { memory.apiHeaders(expectedAccount, it) })
            var recovered = false
            val session = JwxtSessionManager(api) { account ->
                if (!recover || account.isBlank()) throw JwxtAuthenticationRequired()
                CasAuthManager(app, api).restoreJwxt(account)
                recovered = true
            }
            val (student, calendar, fetched) = session.withSession(expectedAccount) { student ->
                memory.confirmed(student.account)
                CampusSyncStatus.phase(SyncStage.READING,"正在读取真实课表…")
                val calendar = api.calendar()
                Triple(student, calendar, api.schedule(student.account, calendar.first.semester, calendar.second))
            }
            val account = student.account
            val term = calendar.first
            val db = app.database
            CampusSyncStatus.phase(SyncStage.WRITING,"正在保存课表…")
            val scheduleId = db.withTransaction {
                val ledger = db.scvtcStateDao()
                val key = profileKey(account, term.semester)
                val existingId = ledger.get(key)?.toIntOrNull()
                val validId = existingId?.takeIf { id -> db.scheduleProfileDao().getProfiles().any { it.id == id } }
                val id = validId ?: db.scheduleProfileDao().upsertProfile(ScheduleProfileEntity(
                    name = "川职 ${term.semester} · ${account.takeLast(4)}"
                )).toInt().also { ledger.put(ScvtcState(key, it.toString())) }
                val oldConfig = db.configDao().getConfig(id)
                val count = calendar.second.max()
                val current = (ChronoUnit.WEEKS.between(term.firstMonday, LocalDate.now()) + 1).toInt().coerceIn(1, count)
                val config = oldConfig ?: ScheduleConfigEntity(id, count, current, 10,
                    termStartDate = term.firstMonday.toString(), autoCurrentWeek = true,
                    termState = if (LocalDate.now() < term.firstMonday) ScheduleTermState.UPCOMING else ScheduleTermState.ACTIVE)
                db.configDao().upsertConfig(config.copy(totalWeeks = maxOf(config.totalWeeks, count)))
                val incoming = fetched.extraction.meetings.map { it.nativeCourse(id) }
                val maxNode = incoming.flatMap { it.periods }.maxOrNull() ?: 12
                val periods = db.configDao().getPeriods(id).associateBy { it.periodIndex }
                db.configDao().upsertPeriods((1..maxOf(maxNode, periods.keys.maxOrNull() ?: 0)).map { node ->
                    periods[node] ?: PeriodEntity(node, "", "", id) // No invented school bell times.
                })
                merge(app, id, incoming)
                if (old == null || old.username != account || old.scheduleId != id) db.scheduleProfileDao().activateProfile(id)
                id
            }
            memory.confirmed(account)
            val updated = AutoRefreshScheduleProfile(SCHOOL, "四川职业技术学院", "scvtc-native-v1",
                "官方教务接口", account, "", scheduleId, automatic = old?.automatic ?: true,
                frequencyMinutes = old?.frequencyMinutes ?: AutoRefreshFrequency.DailyMinutes,
                avatarPath = old?.takeIf { it.username == account }?.avatarPath,
                lastRefreshAt = System.currentTimeMillis(),
                lastResult = (if (recovered) "会话已自动恢复；" else "") + "已验证学生身份并同步 ${fetched.extraction.meetings.size} 项课程安排；本地编辑保留")
            AutoRefreshScheduleStore.save(app, updated)
            CampusSyncStatus.success(app,updated.lastRefreshAt)
            AutoRefreshScheduleWorker.updateSchedule(app, updated)
            val snapshot = app.repository.activeSnapshot()
            NotificationScheduler.refreshToday(app, snapshot.courses, snapshot.config, snapshot.periods)
            TodayCoursesWidgetProvider.refreshAll(app)
            AutoRefreshOutcome(true, updated.lastResult, updated)
        } catch (e: CancellationException) {
            if (e !is TimeoutCancellationException) {
                CampusSyncStatus.phase(SyncStage.CACHE,"已停止 · 使用本机缓存")
                throw e
            }
            CampusSyncStatus.failure(app)
            failure(app, "登录恢复超时；已保存的课表仍可使用")
        } catch (e: Exception) {
            CampusSyncStatus.failure(app,e is JwxtAuthenticationRequired)
            val message = if (e is JwxtAuthenticationRequired) "学校需要补充认证，请打开官方登录；离线课表已保留"
                else "同步未完成，离线课表已保留。请检查网络或稍后重试"
            failure(app, message)
        }
    }

    private fun failure(app: Context, message: String): AutoRefreshOutcome {
        AutoRefreshScheduleStore.update(app) { it.copy(lastResult = message) }
        return AutoRefreshOutcome(false, message, AutoRefreshScheduleStore.load(app))
    }

    suspend fun disconnect(context: Context) {
        synchronized(this) { loginJob?.cancel() }
        coordinator.withLock {
        AutoRefreshScheduleStore.load(context)?.let { OfficialLoginMemory(context).forget(it.username) }
        AutoRefreshScheduleWorker.updateSchedule(context, null)
        AutoRefreshScheduleStore.clear(context)
        withContext(Dispatchers.Main) {
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.CookieManager.getInstance().flush()
            android.webkit.WebStorage.getInstance().deleteAllData()
        }
        }
    }

    private suspend fun merge(app: CourseScheduleApp, id: Int, incoming: List<CourseEntity>) {
        val dao = app.database.courseDao()
        val ledger = app.database.scvtcStateDao()
        val key = "courses:$id"
        val previous = ledger.get(key)?.let(::JSONObject) ?: JSONObject()
        val current = dao.getCourses(id).associateBy { it.id }
        val next = JSONObject()
        // School keys group repeated arrangements so odd/even variants cannot overwrite one another.
        val groups = incoming.groupBy(::schoolKey)
        for ((base, rows) in groups) for ((index, row) in rows.sortedBy { it.weeks.joinToString(",") }.withIndex()) {
            val item = "$base:$index"
            val saved = previous.optJSONObject(item)
            val before = saved?.optLong("id")?.let(current::get)
            if (saved != null && (before == null || fingerprint(before) != saved.optString("hash"))) {
                next.put(item, saved) // User deletion or edit is authoritative.
                continue
            }
            val duplicate = if (saved == null) current.values.firstOrNull {
                it.name == row.name && it.weekday == row.weekday && it.periods == row.periods &&
                    it.weeks.sorted() == row.weeks && it.teacher == row.teacher && it.location == row.location
            } else null
            if (duplicate != null) continue // Migrated/manual rows remain user-owned.
            val value = row.copy(id = before?.id ?: 0)
            val rowId = dao.insertCourse(value)
            next.put(item, JSONObject().put("id", rowId).put("hash", fingerprint(value.copy(id = rowId))))
        }
        previous.keys().forEach { keyName ->
            if (!next.has(keyName)) {
                val saved = previous.getJSONObject(keyName)
                val before = current[saved.getLong("id")]
                if (before != null && fingerprint(before) == saved.getString("hash")) dao.deleteCourse(before.id)
                else next.put(keyName, saved)
            }
        }
        ledger.put(ScvtcState(key, next.toString()))
    }
}
