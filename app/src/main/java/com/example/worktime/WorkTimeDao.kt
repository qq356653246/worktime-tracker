package com.example.worktime

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkTimeDao {
    
    @Query("SELECT * FROM work_records WHERE date = :date AND checkOutTime IS NULL LIMIT 1")
    fun getUnfinishedRecord(date: String): WorkRecord?
    
    @Query("SELECT * FROM work_records ORDER BY date DESC, checkInTime DESC")
    fun getAllRecords(): List<WorkRecord>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecord(record: WorkRecord): Long
    
    @Update
    fun updateRecord(record: WorkRecord)
    
    @Query("DELETE FROM work_records WHERE date = :date")
    fun deleteRecordsForDate(date: String)
}
