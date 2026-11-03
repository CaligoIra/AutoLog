package com.autolog.app.trip

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: Trip): Long

    @Delete
    suspend fun delete(trip: Trip)

    @Query("SELECT * FROM trips WHERE endedAt IS NULL ORDER BY id DESC LIMIT 1")
    suspend fun openTrip(): Trip?

    @Query("SELECT * FROM trips WHERE endedAt IS NULL ORDER BY id DESC LIMIT 1")
    fun observeOpenTrip(): Flow<Trip?>

    @Query("SELECT * FROM trips WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeFinishedTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun get(id: Long): Trip?

    @Query("SELECT * FROM trips WHERE endedAt IS NOT NULL ORDER BY endedAt DESC LIMIT 1")
    suspend fun lastFinishedTrip(): Trip?

    /** 이 운행 바로 앞의 끝난 운행. 차는 주차 중에 움직이지 않으므로 그 도착지가 이번 출발지다. */
    @Query("SELECT * FROM trips WHERE endedAt IS NOT NULL AND id < :id ORDER BY id DESC LIMIT 1")
    suspend fun previousTrip(id: Long): Trip?

    // 아래는 한 칸씩만 바꾼다. 묻거나 위치를 확인하는 동안 같은 행의 다른 칸이 바뀌어도 덮어쓰지 않도록 Trip 전체를 update하지 않는다.
    @Query("UPDATE trips SET endedAt = :endedAt, endKm = :endKm WHERE id = :id")
    suspend fun finish(id: Long, endedAt: Long, endKm: Float)

    /** 끝낸 운행을 다시 연다. 도착 장소는 진짜로 도착했을 때 다시 정한다. */
    @Query("UPDATE trips SET endedAt = NULL, endKm = NULL, endPlace = NULL, type = :type, purpose = :purpose WHERE id = :id")
    suspend fun reopen(id: Long, type: TripType?, purpose: String?)

    @Query("UPDATE trips SET destination = :destination WHERE id = :id")
    suspend fun setDestination(id: Long, destination: String)

    @Query("UPDATE trips SET purpose = :purpose WHERE id = :id")
    suspend fun setPurpose(id: Long, purpose: String)

    @Query("UPDATE trips SET type = :type WHERE id = :id")
    suspend fun setType(id: Long, type: TripType)

    @Query("UPDATE trips SET startPlace = :place WHERE id = :id")
    suspend fun setStartPlace(id: Long, place: Place)

    @Query("UPDATE trips SET endPlace = :place WHERE id = :id")
    suspend fun setEndPlace(id: Long, place: Place)

    @Query("UPDATE trips SET driverName = :name, driverDepartment = :department WHERE id = :id")
    suspend fun setDriver(id: Long, name: String?, department: String?)
}

/** 목적을 저장하고, 구분이 아직 없으면 목적으로 정한다(화면에서 고른 구분은 그대로 둔다). 정해진 구분을 돌려준다. */
suspend fun TripDao.savePurpose(trip: Trip, purpose: String): TripType {
    setPurpose(trip.id, purpose)
    return trip.type ?: TripType.fromPurpose(purpose).also { setType(trip.id, it) }
}

@Database(entities = [Trip::class], version = 4, exportSchema = false)
abstract class AutoLogDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
}

/** v2: 운행 목적(purpose) 칸 추가. 이미 쌓인 기록은 목적이 빈 채로 남는다. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE trips ADD COLUMN purpose TEXT")
    }
}

/** v3: 출발·도착 장소와 업무 구분 칸 추가. 이미 쌓인 기록은 구분 안 됨으로 남아 화면에서 고를 수 있다. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE trips ADD COLUMN startPlace TEXT")
        db.execSQL("ALTER TABLE trips ADD COLUMN endPlace TEXT")
        db.execSQL("ALTER TABLE trips ADD COLUMN type TEXT")
    }
}

/** v4: 운행별 사용자(성명·부서) 칸 추가. 이미 쌓인 기록은 기본 운전자로 채워진다. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE trips ADD COLUMN driverName TEXT")
        db.execSQL("ALTER TABLE trips ADD COLUMN driverDepartment TEXT")
    }
}
