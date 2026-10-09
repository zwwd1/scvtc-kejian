package cn.scvtc.campus

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Transactional ownership ledger, containing no passwords, cookies or identity payloads. */
@Entity(tableName = "scvtc_sync_state")
data class ScvtcState(@PrimaryKey val key: String, val value: String)

@Dao
interface ScvtcStateDao {
    @Query("SELECT value FROM scvtc_sync_state WHERE `key` = :key")
    suspend fun get(key: String): String?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(value: ScvtcState)
}

val SCVTC_MIGRATION_42_43 = object : Migration(42, 43) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS scvtc_sync_state (`key` TEXT NOT NULL, value TEXT NOT NULL, PRIMARY KEY(`key`))")
    }
}
