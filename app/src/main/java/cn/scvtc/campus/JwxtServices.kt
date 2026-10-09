package cn.scvtc.campus

import cn.scvtc.campus.core.NativeRecord
import kotlinx.serialization.json.*

/** The school's confirmed JSON contracts; unrelated services stay in the official website. */
object JwxtServices {
    val endpoints=mapOf(
        "grades" to JwxtApi.BASE+"score/scorequerymanage/studentQuery",
        "credits" to JwxtApi.BASE+"scheme/majorSchemaCustomize/queryStudentGraduationCredit",
    )
    fun pageBody(module:String,page:Int)=buildJsonObject {
        require(module=="grades")
        put("pageNo",page);put("pageSize",100);put("total",0)
        put("param",buildJsonObject{put("scoreInvalid","0")})
    }.toString()
    fun record(module:String,row:JsonObject,account:String):NativeRecord {
        require(module=="grades")
        fun value(key:String)=(row[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
        listOf("studentCode","studentId").forEach { key ->
            value(key).takeIf(String::isNotBlank)?.let{check(it==account){"ACCOUNT_MISMATCH：成绩账号不匹配"}}
        }
        val names=linkedMapOf("courseName" to "课程名称","courseCode" to "课程代码","semesterId" to "学期",
            "studentScoreFinalValue" to "成绩","courseCredit" to "学分","acquireCourseCredit" to "获得学分",
            "gradePoint" to "绩点","studyTypeName" to "修读类型","natureName" to "课程性质")
        val fields=linkedMapOf<String,String>()
        names.forEach{(key,label)->value(key).takeIf(String::isNotBlank)?.let{fields[label]=it}}
        if(fields["成绩"].isNullOrBlank())value("scoreValue").takeIf(String::isNotBlank)?.let{fields["成绩"]=it}
        check(value("courseName").isNotBlank() && !fields["成绩"].isNullOrBlank()){"PAGE_CHANGED：成绩字段缺失"}
        return NativeRecord(value("courseName"),fields)
    }
}
