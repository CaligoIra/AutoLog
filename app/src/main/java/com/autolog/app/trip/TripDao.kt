package com.autolog.app.trip

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
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
}

@Database(entities = [Trip::class], version = 1, exportSchema = false)
abstract class AutoLogDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
}
