package com.example.worktime

import androidx.room.*

@Dao
interface AttendanceDao {
    
    @Query("SELECT * FROM attendance_records WHERE date = :date ORDER BY checkInTime DESC")
    fun getTodayRecords(date: String): List<AttendanceRecord>
    
    @Query("SELECT * FROM attendance_records ORDER BY date DESC, checkInTime DESC")
    fun getAllRecords(): List<AttendanceRecord>
    
    @Query("SELECT * FROM attendance_records WHERE date = :date AND checkOutTime IS NULL LIMIT 1")
    fun getUnfinishedRecord(date: String): AttendanceRecord?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecord(record: AttendanceRecord): Long
    
    @Update
    fun updateRecord(record: AttendanceRecord)
    
    @Query("DELETE FROM attendance_records WHERE date = :date")
    fun deleteRecordsForDate(date: String)
}
