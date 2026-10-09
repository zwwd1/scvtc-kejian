package cn.scvtc.campus

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import androidx.room.withTransaction
import com.xiaomanjun.sleepdownschedule.CourseScheduleApp
import com.xiaomanjun.sleepdownschedule.model.*
import com.xiaomanjun.sleepdownschedule.domain.schedule.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

/** Reads the old app's database without altering or removing it. The destination
 * import and completion marker commit together, including on process death. */
internal object ScvtcLegacyMigration {
    private val mutex = Mutex()
    suspend fun run(app: CourseScheduleApp) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val db = app.database
            if (db.scvtcStateDao().get("legacy:v1") != null) return@withContext
            val source = app.getDatabasePath("openwakeup.db")
            if (!source.isFile) {
                db.scvtcStateDao().put(ScvtcState("legacy:v1", "no-source"))
                return@withContext
            }
            SQLiteDatabase.openDatabase(source.path, null, SQLiteDatabase.OPEN_READONLY).use { old ->
                db.withTransaction {
                    if (db.scvtcStateDao().get("legacy:v1") != null) return@withTransaction
                    var lastId: Int? = null
                    old.rawQuery("SELECT * FROM tables ORDER BY id", null).use { tables ->
                        while (tables.moveToNext()) {
                            val oldId = tables.long("id")
                            val id = db.scheduleProfileDao().upsertProfile(ScheduleProfileEntity(
                                name = tables.string("tableName").ifBlank { "迁移课表" }
                            )).toInt()
                            val adjustments = linkedMapOf<String, ScheduleAdjustment>()
                            if (old.hasTable("schedule_shifts")) old.rawQuery(
                                "SELECT * FROM schedule_shifts WHERE tableId=? ORDER BY createdAt,id", arrayOf(oldId.toString())
                            ).use { shifts -> while (shifts.moveToNext()) {
                                val from = shifts.string("fromDate")
                                val to = shifts.string("toDate")
                                LocalDate.parse(from); LocalDate.parse(to)
                                val sourceDate = if (adjustments.containsKey(from)) adjustments[from]?.sourceDate else from
                                adjustments[from] = ScheduleAdjustment(from, null, "迁移调课")
                                adjustments[to] = ScheduleAdjustment(to, sourceDate, "迁移调课")
                            } }
                            val start = tables.string("startDate").takeIf { runCatching { LocalDate.parse(it) }.isSuccess }
                            val overrideWeek = tables.int("currentWeekOverride")
                            db.configDao().upsertConfig(ScheduleConfigEntity(id = id,
                                totalWeeks = tables.int("maxWeek").coerceAtLeast(1),
                                currentWeek = overrideWeek.coerceAtLeast(1), notificationLeadMinutes = 10,
                                termStartDate = start, autoCurrentWeek = overrideWeek == 0 && start != null,
                                scheduleAdjustmentsJson = encodeScheduleAdjustments(adjustments.values.toList())))
                            val validTimes = mutableMapOf<Int, PeriodEntity>()
                            old.rawQuery("SELECT * FROM time_details WHERE timeTableId=?", arrayOf(tables.long("timeTableId").toString())).use { times ->
                                while (times.moveToNext()) {
                                    val node = times.int("node")
                                    if (node > 0) validTimes[node] = PeriodEntity(node, clock(times.string("startTime")), clock(times.string("endTime")), id)
                                }
                            }
                            var maxNode = tables.int("nodes").coerceAtLeast(1)
                            old.rawQuery("SELECT c.*,d.*,d.id AS detailId FROM courses c JOIN course_details d ON d.courseId=c.id WHERE c.tableId=? ORDER BY d.id", arrayOf(oldId.toString())).use { rows ->
                                while (rows.moveToNext()) {
                                    val first = rows.int("startNode")
                                    val end = first + rows.int("step").coerceAtLeast(1) - 1
                                    require(first > 0 && end <= 100 && rows.int("day") in 1..7)
                                    maxNode = maxOf(maxNode, end)
                                    val type = rows.int("type")
                                    val weeks = (rows.int("startWeek")..rows.int("endWeek")).filter { it > 0 && (type == 0 || it % 2 == type % 2) }
                                    val own = rows.int("ownTime") == 1
                                    val customStart = if (own) clock(rows.string("startTime")).ifBlank { null } else null
                                    val customEnd = if (own) clock(rows.string("endTime")).ifBlank { null } else null
                                    db.courseDao().insertCourse(CourseEntity(
                                        name = rows.string("courseName"), teacher = rows.string("teacher").ifBlank { null },
                                        location = rows.string("room").ifBlank { null }, weekday = rows.int("day"),
                                        periods = (first..end).toList(), weeks = weeks, weekParity = WeekParity.ALL,
                                        note = rows.string("note").ifBlank { null }, customStartTime = customStart, customEndTime = customEnd,
                                        customColorArgb = runCatching { Color.parseColor(rows.string("color")).toLong() and 0xffffffffL }.getOrNull(),
                                        scheduleId = id))
                                }
                            }
                            db.configDao().upsertPeriods((1..maxNode).map { validTimes[it] ?: PeriodEntity(it, "", "", id) })
                            if (old.hasTable("campus_tables")) old.rawQuery("SELECT account,semester FROM campus_tables WHERE tableId=?", arrayOf(oldId.toString())).use { bindings ->
                                while (bindings.moveToNext()) {
                                    db.scvtcStateDao().put(ScvtcState(ScvtcNativeBridge.profileKey(bindings.string("account"), bindings.string("semester")), id.toString()))
                                    lastId = id
                                }
                            }
                            db.scvtcStateDao().put(ScvtcState("legacy:table:$oldId", id.toString()))
                        }
                    }
                    lastId?.let { db.scheduleProfileDao().activateProfile(it) }
                    db.scvtcStateDao().put(ScvtcState("legacy:v1", "complete"))
                }
            }
        }
    }
    private fun clock(value: String) = value.takeIf { runCatching { LocalTime.parse(it) }.isSuccess }.orEmpty()
    private fun Cursor.string(name: String): String = getColumnIndex(name).takeIf { it >= 0 }?.let { getString(it) }.orEmpty()
    private fun Cursor.int(name: String): Int = string(name).toIntOrNull() ?: 0
    private fun Cursor.long(name: String): Long = string(name).toLongOrNull() ?: 0
    private fun SQLiteDatabase.hasTable(name: String) = rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", arrayOf(name)).use { it.moveToFirst() }
}
