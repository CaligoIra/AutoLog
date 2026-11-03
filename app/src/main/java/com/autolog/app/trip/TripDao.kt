package com.autolog.app.trip

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: Trip): Long

    @Update
    suspend fun update(trip: Trip)

    @Delete
    suspend fun delete(trip: Trip)

    @Query("SELECT * FROM trips WHERE endedAt IS NULL ORDER BY id DESC LIMIT 1")
    suspend fun openTrip(): Trip?

    @Query("SELECT * FROM trips WHERE endedAt IS NULL ORDER BY id DESC LIMIT 1")
    fun observeOpenTrip(): Flow<Trip?>

    @Query("SELECT * FROM trips WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeFinishedTrips(): Flow<List<Trip>>

    // 목적 칸만 바꾼다. 묻는 동안 같은 행의 다른 칸이 바뀌어도 덮어쓰지 않도록 Trip 전체를 update하지 않는다.
    @Query("UPDATE trips SET purpose = :purpose WHERE id = :id")
    suspend fun setPurpose(id: Long, purpose: String)
}

@Database(entities = [Trip::class], version = 2, exportSchema = false)
abstract class AutoLogDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
}

/** v2: 운행 목적(purpose) 칸 추가. 이미 쌓인 기록은 목적이 빈 채로 남는다. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE trips ADD COLUMN purpose TEXT")
    }
}
