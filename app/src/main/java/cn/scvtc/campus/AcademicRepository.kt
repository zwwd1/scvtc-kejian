package cn.scvtc.campus

import androidx.room.withTransaction
import cn.scvtc.campus.core.NativeRecord
import com.xiaomanjun.sleepdownschedule.CourseScheduleApp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class AcademicSnapshot(
    val account:String,val currentTerm:String,val terms:List<String>,val grades:List<NativeRecord>,
    val requiredCredits:String,val fetchedAt:Long,val requiredCreditsFetchedAt:Long=0,
)
internal data class AcademicReadState(val account:String="",val message:String="",val busy:Boolean=false,val authRequired:Boolean=false)

/** A separate query cache inside the existing Room state table. No course rows
 * or hand-edited schedule profiles are touched by grades/credits refresh. */
internal object AcademicRepository {
    private val json=Json{ignoreUnknownKeys=true;encodeDefaults=true}
    private val mutableState=MutableStateFlow(AcademicReadState())
    val state=mutableState.asStateFlow()
    private var refreshJob:Job?=null
    @Synchronized fun requestRefresh(app:CourseScheduleApp,account:String) {
        if(refreshJob?.isActive==true)return
        mutableState.value=AcademicReadState(account,"等待学校同步…",true)
        refreshJob=app.applicationScope.launch{refresh(app,account)}
    }
    private fun key(account:String,term:String)="academic:"+scvtcKey("$account|$term")
    suspend fun cached(app:CourseScheduleApp,account:String,term:String="all"):AcademicSnapshot? {
        val encrypted=app.database.scvtcStateDao().get(key(account,term))?:return null
        val opened=OfficialLoginMemory(app).openAcademic(account,term,encrypted)?:return null
        return json.decodeFromString<AcademicSnapshot>(opened).takeIf{it.account==account}
    }
    suspend fun refresh(app:CourseScheduleApp,account:String):Boolean=withContext(Dispatchers.IO) { ScvtcNativeBridge.coordinator.withLock {
        require(account.isNotBlank())
        CampusSyncStatus.begin(app,account,"成绩与学分",cached(app,account)?.fetchedAt?:0)
        CampusSyncStatus.phase(SyncStage.AUTHENTICATING,"正在认证学校账号…")
        mutableState.value=AcademicReadState(account,"正在认证学校账号…",true)
        try {
            val memory=OfficialLoginMemory(app)
            memory.restoreSession(account)
            val api=JwxtApi(headers={memory.apiHeaders(account,it)})
            val session=JwxtSessionManager(api){CasAuthManager(app,api).restoreJwxt(it)}
            val previous=cached(app,account)
            var requirementUpdated=false
            val snapshot=session.withSession(account){student->
                memory.confirmed(student.account)
                mutableState.value=AcademicReadState(account,"正在逐页读取成绩与学分…",true)
                CampusSyncStatus.phase(SyncStage.READING,"正在读取成绩与学分…")
                val term=api.calendar().first.semester
                val terms=api.semesters()
                val grades=api.service("grades",account,term).records
                val requirement=try {
                    api.service("credits",account,term).records.single().fields.getValue("毕业学分要求").also{requirementUpdated=true}
                }catch(e:CancellationException){throw e}
                catch(e:JwxtAuthenticationRequired){throw e}
                catch(e:Exception){previous?.requiredCredits.orEmpty()}
                val fetchedAt=System.currentTimeMillis()
                val requirementAt=if(requirementUpdated)fetchedAt else previous?.let{it.requiredCreditsFetchedAt.takeIf{date->date>0}?:it.fetchedAt}?:0
                AcademicSnapshot(account,term,(terms+grades.mapNotNull{it.fields["学期"]}).distinct().sortedDescending(),grades,requirement,fetchedAt,requirementAt)
            }
            mutableState.value=AcademicReadState(account,"正在加密保存本机记录…",true)
            CampusSyncStatus.phase(SyncStage.WRITING,"正在保存成绩与学分…")
            val copies=listOf("all" to snapshot)+snapshot.terms.map { term ->
                term to snapshot.copy(grades=snapshot.grades.filter{it.fields["学期"]==term})
            }
            val encrypted=copies.map{(term,data)->ScvtcState(key(account,term),memory.sealAcademic(account,term,json.encodeToString(data)))}
            app.database.withTransaction{encrypted.forEach{app.database.scvtcStateDao().put(it)}}
            CampusSyncStatus.success(app,snapshot.fetchedAt)
            mutableState.value=AcademicReadState(account,if(requirementUpdated)"已同步 ${snapshot.grades.size} 条真实成绩；毕业要求来自学校接口" else "已同步 ${snapshot.grades.size} 条成绩；毕业要求未更新，保留已有信息")
            true
        } catch(e:CancellationException){
            CampusSyncStatus.phase(SyncStage.CACHE,"已停止 · 使用本机缓存")
            mutableState.value=AcademicReadState(account,"读取已停止；保留上次缓存")
            throw e
        } catch(e:Exception){
            CampusSyncStatus.failure(app,e is JwxtAuthenticationRequired)
            mutableState.value=AcademicReadState(account,if(e is JwxtAuthenticationRequired)"需要补充学校认证；保留上次缓存" else "读取未完成；保留上次缓存，请稍后重试",authRequired=e is JwxtAuthenticationRequired)
            false
        }
    } }
}
