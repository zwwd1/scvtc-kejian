package cn.scvtc.campus

import android.content.Context
import android.net.ConnectivityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.DateFormat
import java.util.Date

internal enum class SyncStage { NONE, PREPARING, AUTHENTICATING, READING, WRITING, SUCCESS, FAILED, OFFLINE, AUTH_REQUIRED, CACHE }
internal data class CampusSyncState(
    val stage:SyncStage=SyncStage.NONE,val operation:String="课表",val lastAttempt:Long=0,
    val lastSuccess:Long=0,val message:String="尚未同步",
) {
    val busy get()=stage in setOf(SyncStage.PREPARING,SyncStage.AUTHENTICATING,SyncStage.READING,SyncStage.WRITING)
    val label get()=if(stage==SyncStage.SUCCESS)"同步成功 · ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(lastSuccess))}" else message
}
/** Only coordinators publish phases. A successful timestamp follows a Room commit. */
internal object CampusSyncStatus {
    private val mutableState=MutableStateFlow(CampusSyncState())
    val state=mutableState.asStateFlow()
    private var scopeKey=""
    fun restore(context:Context,account:String,operation:String="课表",previousSuccess:Long=0) {
        if(mutableState.value.busy)return
        scopeKey=scvtcKey("$account|$operation")
        val prefs=context.getSharedPreferences("campus_sync_status",Context.MODE_PRIVATE)
        val success=prefs.getLong("success:$scopeKey",previousSuccess)
        mutableState.value=CampusSyncState(if(success>0)SyncStage.CACHE else SyncStage.NONE,operation,
            prefs.getLong("attempt:$scopeKey",0),success,if(success>0)"本机缓存 · 点击刷新" else "尚未同步 · 点击连接")
    }
    fun begin(context:Context,account:String,operation:String="课表",previousSuccess:Long=0) {
        restore(context,account,operation,previousSuccess)
        val now=System.currentTimeMillis()
        mutableState.value=mutableState.value.copy(stage=SyncStage.PREPARING,lastAttempt=now,message="准备同步$operation…")
        context.getSharedPreferences("campus_sync_status",Context.MODE_PRIVATE).edit().putLong("attempt:$scopeKey",now).apply()
    }
    fun phase(stage:SyncStage,message:String){mutableState.value=mutableState.value.copy(stage=stage,message=message)}
    fun success(context:Context,time:Long){
        context.getSharedPreferences("campus_sync_status",Context.MODE_PRIVATE).edit().putLong("success:$scopeKey",time).apply()
        mutableState.value=mutableState.value.copy(stage=SyncStage.SUCCESS,lastSuccess=time,message="同步成功")
    }
    fun failure(context:Context,authentication:Boolean=false){
        val offline=context.getSystemService(ConnectivityManager::class.java).activeNetwork==null
        phase(when{authentication->SyncStage.AUTH_REQUIRED;offline->SyncStage.OFFLINE;else->SyncStage.FAILED},
            when{authentication->"需补充认证 · 缓存保留";offline->"离线 · 使用上次缓存";else->"同步失败 · 点击重试"})
    }
}
