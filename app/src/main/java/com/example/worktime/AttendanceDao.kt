package com.example.worktime

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    
    @Query("SELECT * FROM attendance_records WHERE date = :date ORDER BY checkInTime DESC")
    fun getTodayRecords(date: String): Flow<List<AttendanceRecord>>
    
    @Query("SELECT * FROM attendance_records ORDER BY date DESC, checkInTime DESC")
    fun getAllRecords(): Flow<List<AttendanceRecord>>
    
    @Query("SELECT * FROM attendance_records WHERE date = :date AND checkOutTime IS NULL LIMIT 1")
    suspend fun getUnfinishedRecord(date: String): AttendanceRecord?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord): Long
    
    @Update
    suspend fun updateRecord(record: AttendanceRecord)
    
    @Query("DELETE FROM attendance_records WHERE date = :date")
    suspend fun deleteRecordsForDate(date: String)
    
    @Query("SELECT SUM(duration) FROM attendance_records WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalDuration(startDate: String, endDate: String): Long?
}
