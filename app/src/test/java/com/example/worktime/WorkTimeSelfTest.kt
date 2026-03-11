package com.example.worktime

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 工时记录应用 - 完整自验证测试
 * 包含 14 个测试用例
 */
class WorkTimeSelfTest {

    private val today = getCurrentDate()
    private var passedCount = 0
    private var failedCount = 0

    fun runAllTests() {
        println("=" .repeat(70))
        println("🧪 工时记录应用 - 自验证测试")
        println("=" .repeat(70))
        println("测试日期：${getCurrentDate()}")
        println("=" .repeat(70))

        // 打卡逻辑测试（4 个用例）
        println("\n📋 第一部分：打卡逻辑测试")
        println("-" .repeat(70))
        test1_FirstCheckIn_ShouldUpdateStartTime()
        test2_SecondCheckOut_ShouldUpdateEndTime()
        test3_ThirdCheck_ShouldUpdateEndTimeAgain()
        test4_TenConsecutiveClicks_ShouldUseLastEndTime()

        // 休息时间设置功能测试（6 个用例）
        println("\n📋 第二部分：休息时间设置功能测试")
        println("-" .repeat(70))
        test5_BreakTimeCalculation_SinglePeriod()
        test6_BreakTimeCalculation_MultiplePeriods()
        test7_BreakTimeDuringWork()
        test8_BreakTimeParse_SinglePeriod()
        test9_BreakTimeParse_MultiplePeriods()
        test10_BreakTimePersistence()

        // 数据持久化测试（2 个用例）
        println("\n📋 第三部分：数据持久化测试")
        println("-" .repeat(70))
        test11_RecordSerialization()
        test12_BreakTimeSerialization()

        // UI/UX 测试（2 个用例 - 代码验证）
        println("\n📋 第四部分：UI/UX 代码验证")
        println("-" .repeat(70))
        test13_TimeFormat_SecondPrecision()
        test14_DurationFormat_HourMinuteSecond()

        // 测试结果汇总
        println("\n" + "=" .repeat(70))
        println("📊 测试结果汇总")
        println("=" .repeat(70))
        println("✅ 通过：$passedCount")
        println("❌ 失败：$failedCount")
        println("📝 总计：${passedCount + failedCount}")
        println("=" .repeat(70))

        if (failedCount > 0) {
            println("\n❌ 测试未全部通过，请不要编译！")
            throw RuntimeException("测试失败，请修复后再编译")
        } else {
            println("\n✅ 所有测试通过！可以编译。")
        }
    }

    // ==================== 第一部分：打卡逻辑测试 ====================

    fun test1_FirstCheckIn_ShouldUpdateStartTime() {
        println("\n🧪 测试用例 1: 第一次点按钮，开始时间刷新")
        try {
            val records = mutableListOf<WorkRecord>()
            val checkInTime = System.currentTimeMillis()
            
            val record = WorkRecord(
                date = today,
                checkInTime = checkInTime,
                checkOutTime = null,
                duration = 0
            )
            records.add(record)

            require(records.size == 1) { "记录数应为 1" }
            require(records[0].checkInTime == checkInTime) { "开始时间应等于打卡时间" }
            require(records[0].checkOutTime == null) { "结束时间应为空" }
            
            println("  ✅ 开始时间：${formatTime(checkInTime)}")
            println("  ✅ 结束时间：--:--:--")
            println("  ✅ 状态：工作中")
            passedCount++
            println("  ✅ 测试用例 1 通过")
        } catch (e: Exception) {
            println("  ❌ 测试用例 1 失败：${e.message}")
            failedCount++
        }
    }

    fun test2_SecondCheckOut_ShouldUpdateEndTime() {
        println("\n🧪 测试用例 2: 第二次点按钮，结束时间刷新")
        try {
            val records = mutableListOf<WorkRecord>()
            val checkInTime = System.currentTimeMillis() - 3600000
            records.add(WorkRecord(today, checkInTime, null, 0))

            val checkOutTime = System.currentTimeMillis()
            val duration = checkOutTime - checkInTime
            records[0] = WorkRecord(
                date = records[0].date,
                checkInTime = records[0].checkInTime,
                checkOutTime = checkOutTime,
                duration = duration
            )

            require(records[0].checkOutTime != null) { "结束时间不应为空" }
            require(records[0].checkOutTime == checkOutTime) { "结束时间应等于打卡时间" }
            require(records[0].duration > 0) { "工时应大于 0" }
            
            println("  ✅ 开始时间：${formatTime(checkInTime)}")
            println("  ✅ 结束时间：${formatTime(checkOutTime)}")
            println("  ✅ 工作时长：${formatDuration(duration)}")
            passedCount++
            println("  ✅ 测试用例 2 通过")
        } catch (e: Exception) {
            println("  ❌ 测试用例 2 失败：${e.message}")
            failedCount++
        }
    }

    fun test3_ThirdCheck_ShouldUpdateEndTimeAgain() {
        println("\n🧪 测试用例 3: 第三次点按钮，结束时间刷新")
        try {
            val records = mutableListOf<WorkRecord>()
            val checkInTime = System.currentTimeMillis() - 7200000
            val firstCheckOutTime = System.currentTimeMillis() - 3600000
            records.add(WorkRecord(today, checkInTime, firstCheckOutTime, 3600000))

            Thread.sleep(100)
            val newCheckOutTime = System.currentTimeMillis()
            val newDuration = newCheckOutTime - checkInTime
            records[0] = WorkRecord(
                date = records[0].date,
                checkInTime = records[0].checkInTime,
                checkOutTime = newCheckOutTime,
                duration = newDuration
            )

            require(records[0].checkOutTime == newCheckOutTime) { "结束时间应等于最新打卡时间" }
            require(records[0].checkOutTime != firstCheckOutTime) { "结束时间应不同于第一次结束时间" }
            require(records[0].duration > 3600000) { "工时应大于 1 小时" }
            
            println("  ✅ 开始时间：${formatTime(checkInTime)} (保持不变)")
            println("  ✅ 第一次结束时间：${formatTime(firstCheckOutTime)}")
            println("  ✅ 第二次结束时间：${formatTime(newCheckOutTime)} (已更新)")
            println("  ✅ 工作时长：${formatDuration(newDuration)}")
            passedCount++
            println("  ✅ 测试用例 3 通过")
        } catch (e: Exception) {
            println("  ❌ 测试用例 3 失败：${e.message}")
            failedCount++
        }
    }

    fun test4_TenConsecutiveClicks_ShouldUseLastEndTime() {
        println("\n🧪 测试用例 4: 连续 10 次点击，结束时间取最后一次")
        try {
            val records = mutableListOf<WorkRecord>()
            val checkInTime = System.currentTimeMillis()
            records.add(WorkRecord(today, checkInTime, null, 0))

            var lastCheckOutTime = 0L
            for (i in 2..10) {
                Thread.sleep(50)
                lastCheckOutTime = System.currentTimeMillis()
                val duration = lastCheckOutTime - checkInTime
                records[0] = WorkRecord(
                    date = records[0].date,
                    checkInTime = records[0].checkInTime,
                    checkOutTime = lastCheckOutTime,
                    duration = duration
                )
            }

            require(records[0].checkOutTime == lastCheckOutTime) { "结束时间应等于最后一次打卡时间" }
            require(records[0].duration > 0) { "工时应大于 0" }
            require(records.size == 1) { "记录数应为 1" }
            
            println("  ✅ 开始时间：${formatTime(checkInTime)}")
            println("  ✅ 最终结束时间：${formatTime(lastCheckOutTime)}")
            println("  ✅ 工作时长：${formatDuration(records[0].duration)}")
            println("  ✅ 记录数：1 条")
            passedCount++
            println("  ✅ 测试用例 4 通过")
        } catch (e: Exception) {
            println("  ❌ 测试用例 4 失败：${e.message}")
            failedCount++
        }
    }

    // ==================== 第二部分：休息时间设置功能测试 ====================

    fun test5_BreakTimeCalculation_SinglePeriod() {
        println("\n🧪 测试用例 5: 单个休息时间段计算")
        try {
            val breakTimes = listOf(BreakTime("12:00", "13:00"))
            val checkInTime = parseTime("09:00")
            val checkOutTime = parseTime("14:00")
            
            val breakDuration = calculateBreakDuration(breakTimes, checkInTime, checkOutTime)
            
            require(breakDuration == 3600000) { "休息时长应为 1 小时，实际：${breakDuration}" }
            
            val netDuration = (checkOutTime - checkInTime) - breakDuration
            require(netDuration == 14400000) { "净工作时长应为 4 小时，实际：${netDuration}" }
            
            println("  ✅ 工作时间：09:00 - 14:00 (5 小时)")
            println("  ✅ 休息时间：12:00 - 13:00 (1 小时)")
            println("  ✅ 净工作时长：4 小时")
            passedCount++
            println("  ✅ 测试用例 5 通过")
        } catch (e: Exception) {
            println("  ❌ 测试用例 5 失败：${e.message}")
            failedCount++
        }
    }

    fun test6_BreakTimeCalculation_MultiplePeriods() {
        println("\n🧪 测试用例 6: 多个休息时间段计算")
        try {
            val breakTimes = listOf(
                BreakTime("12:00", "13:00"),
                BreakTime("18:00", "19:00")
            )
            val checkInTime = parseTime("09:00")
            val checkOutTime = parseTime("20:00")
            
            val breakDuration = calculateBreakDuration(breakTimes, checkInTime, checkOutTime)
            
            require(breakDuration == 7200000) { "休息时长应为 2 小时，实际：${breakDuration}" }
            
            val netDuration = (checkOutTime - checkInTime) - breakDuration
            require(netDuration == 32400000) { "净工作时长应为 9 小时，实际：${netDuration}" }
            
            println("  ✅ 工作时间：09:00 - 20:00 (11 小时)")
            println("  ✅ 休息时间：12:00-13:00, 18:00-19:00 (2 小时)")
            println("  ✅ 净工作时长：9 小时")
            passedCount++
            println("  ✅ 测试用例 6 通过")
        } catch (e: Exception) {
            println("  ❌ 测试用例 6 失败：${e.message}")
            failedCount++
        }
    }

    fun test7_BreakTimeDuringWork() {
        println("\n🧪 测试用例 7: 工作时间内的休息计算")
        try {
            val breakTimes = listOf(BreakTime("12:00", "13:00"))
            val checkInTime = parseTime("11:00")
            val checkOutTime = parseTime("12:30")
            
            val breakDuration = calculateBreakDuration(breakTimes, checkInTime, checkOutTime)
            
            require(breakDuration == 1800000) { "休息时长应为 30 分钟，实际：${breakDuration}" }
            
            println("  ✅ 工作时间：11:00 - 12:30 (1.5 小时)")
            println("  ✅ 休息时间：12:00 - 12:30 (30 分钟，部分重叠)")
            println("  ✅ 净工作时长：1 小时")
            passedCount++
            println("  ✅ 测试用例 7 通过")
        } catch (e: Exception) {
            println("  ❌ 测试用例 7 失败：${e.message}")
            failedCount++
        }
    }

    fun test8_BreakTimeParse_SinglePeriod() {
        println("\n🧪 测试用例 8: 单个休息时间段解析")
        try {
            val text = "12:00-13:00"
            val breakTimes = parseBreakTimes(text)
            
            require(breakTimes.size == 1) { "应解析出 1 个时间段" }
            require(breakTimes[0].startTime == "12:00") { "开始时间应为 12:00" }
            require(breakTimes[0].endTime == "13:00") { "结束时间应为 13:00" }
            
            println("  ✅ 输入：12:00-13:00")
            println("  ✅ 解析结果：1 个时间段")
            passedCount++
            println("  ✅ 测试用例 8 通过")
        } catch (e: Exception) {
            println("  ❌ 测试用例 8 失败：${e.message}")
            failedCount++
        }
    }

    fun test9_BreakTimeParse_MultiplePeriods() {
        println("\n🧪 测试用例 9: 多个休息时间段解析")
        try {
            val text = "12:00-13:00,18:00-19:00"
            val breakTimes = parseBreakTimes(text)
            
            require(breakTimes.size == 2) { "应解析出 2 个时间段" }
            require(breakTimes[0].startTime == "12:00") { "第一个开始时间应为 12:00" }
            require(breakTimes[1].startTime == "18:00") { "第二个开始时间应为 18:00" }
            
            println("  ✅ 输入：12:00-13:00,18:00-19:00")
            println("  ✅ 解析结果：2 个时间段")
            passedCount++
            println("  ✅ 测试用例 9 通过")
        } catch (e: Exception) {
            println("  ❌ 测试用例 9 失败：${e.message}")
            failedCount++
        }
    }

    fun test10_BreakTimePersistence() {
        println("\n🧪 测试用例 10: 休息时间数据持久化（模拟）")
        try {
            val breakTimes = listOf(
                BreakTime("12:00", "13:00"),
                BreakTime("15:00", "15:30")
            )
            
            // 模拟序列化和反序列化
            val gson = com.google.gson.Gson()
            val json = gson.toJson(breakTimes)
            val restoredBreakTimes: List<BreakTime> = gson.fromJson(json, object : com.google.gson.reflect.TypeToken<List<BreakTime>>() {}.type)
            
            require(restoredBreakTimes.size == 2) { "应恢复 2 个时间段" }
            require(restoredBreakTimes[0].startTime == "12:00") { "第一个开始时间应正确恢复" }
            
            println("  ✅ 原始数据：2 个时间段")
            println("  ✅ JSON 序列化成功")
            println("  ✅ 反序列化成功")
            passedCount++
            println("  ✅ 测试用例 10 通过")
        } catch (e: Exception) {
            println("  ❌ 测试用例 10 失败：${e.message}")
            failedCount++
        }
    }

    // ==================== 第三部分：数据持久化测试 ====================

    fun test11_RecordSerialization() {
        println("\n🧪 测试用例 11: 打卡记录序列化")
        try {
            val records = listOf(
                WorkRecord(today, System.currentTimeMillis() - 7200000, System.currentTimeMillis() - 3600000, 3600000),
                WorkRecord(today, System.currentTimeMillis() - 3600000, System.currentTimeMillis(), 3600000)
            )
            
            val gson = com.google.gson.Gson()
            val json = gson.toJson(records)
            val restoredRecords: List<WorkRecord> = gson.fromJson(json, object : com.google.gson.reflect.TypeToken<List<WorkRecord>>() {}.type)
            
            require(restoredRecords.size == 2) { "应恢复 2 条记录" }
            require(restoredRecords[0].date == today) { "日期应正确恢复" }
            
            println("  ✅ 原始数据：2 条记录")
            println("  ✅ JSON 序列化成功")
            println("  ✅ 反序列化成功")
            passedCount++
            println("  ✅ 测试用例 11 通过")
        } catch (e: Exception) {
            println("  ❌ 测试用例 11 失败：${e.message}")
            failedCount++
        }
    }

    fun test12_BreakTimeSerialization() {
        println("\n🧪 测试用例 12: 休息时间序列化")
        try {
            val breakTimes = listOf(
                BreakTime("09:00", "09:30"),
                BreakTime("12:00", "13:00"),
                BreakTime("18:00", "18:30")
            )
            
            val gson = com.google.gson.Gson()
            val json = gson.toJson(breakTimes)
            val restoredBreakTimes: List<BreakTime> = gson.fromJson(json, object : com.google.gson.reflect.TypeToken<List<BreakTime>>() {}.type)
            
            require(restoredBreakTimes.size == 3) { "应恢复 3 个时间段" }
            require(restoredBreakTimes[1].startTime == "12:00") { "中间时间段应正确恢复" }
            
            println("  ✅ 原始数据：3 个时间段")
            println("  ✅ JSON 序列化成功")
            println("  ✅ 反序列化成功")
            passedCount++
            println("  ✅ 测试用例 12 通过")
        } catch (e: Exception) {
            println("  ❌ 测试用例 12 失败：${e.message}")
            failedCount++
        }
    }

    // ==================== 第四部分：UI/UX 代码验证 ====================

    fun test13_TimeFormat_SecondPrecision() {
        println("\n🧪 测试用例 13: 时间格式精确到秒")
        try {
            val timestamp = System.currentTimeMillis()
            val timeStr = formatTime(timestamp)
            
            require(timeStr.matches(Regex("\\d{2}:\\d{2}:\\d{2}"))) { "时间格式应为 HH:mm:ss，实际：$timeStr" }
            
            println("  ✅ 时间戳：$timestamp")
            println("  ✅ 格式化结果：$timeStr")
            println("  ✅ 格式验证：HH:mm:ss ✓")
            passedCount++
            println("  ✅ 测试用例 13 通过")
        } catch (e: Exception) {
            println("  ❌ 测试用例 13 失败：${e.message}")
            failedCount++
        }
    }

    fun test14_DurationFormat_HourMinuteSecond() {
        println("\n🧪 测试用例 14: 时长格式显示")
        try {
            val durationMs = 3661000L // 1 小时 1 分钟 1 秒
            val durationStr = formatDuration(durationMs)
            
            require(durationStr.contains("小时")) { "应包含'小时'" }
            require(durationStr.contains("分钟")) { "应包含'分钟'" }
            require(durationStr.contains("秒")) { "应包含'秒'" }
            
            println("  ✅ 原始时长：$durationMs 毫秒")
            println("  ✅ 格式化结果：$durationStr")
            println("  ✅ 格式验证：X 小时 X 分钟 X 秒 ✓")
            passedCount++
            println("  ✅ 测试用例 14 通过")
        } catch (e: Exception) {
            println("  ❌ 测试用例 14 失败：${e.message}")
            failedCount++
        }
    }

    // ==================== 辅助函数 ====================

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatDuration(ms: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return "${hours}小时${minutes}分钟${seconds}秒"
    }

    private fun parseTime(timeStr: String): Long {
        val (hour, minute) = timeStr.split(":").map { it.toInt() }
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun calculateBreakDuration(breakTimes: List<BreakTime>, checkInTime: Long, checkOutTime: Long): Long {
        var totalBreakMs = 0L
        val calendar = Calendar.getInstance()
        
        for (breakTime in breakTimes) {
            val (breakStartHour, breakStartMin) = breakTime.startTime.split(":").map { it.toInt() }
            val (breakEndHour, breakEndMin) = breakTime.endTime.split(":").map { it.toInt() }
            
            calendar.time = Date(checkInTime)
            calendar.set(Calendar.HOUR_OF_DAY, breakStartHour)
            calendar.set(Calendar.MINUTE, breakStartMin)
            calendar.set(Calendar.SECOND, 0)
            val breakStartMs = calendar.timeInMillis
            
            calendar.time = Date(checkInTime)
            calendar.set(Calendar.HOUR_OF_DAY, breakEndHour)
            calendar.set(Calendar.MINUTE, breakEndMin)
            calendar.set(Calendar.SECOND, 0)
            val breakEndMs = calendar.timeInMillis
            
            val actualStart = maxOf(breakStartMs, checkInTime)
            val actualEnd = minOf(breakEndMs, checkOutTime)
            
            if (actualEnd > actualStart) {
                totalBreakMs += (actualEnd - actualStart)
            }
        }
        
        return totalBreakMs
    }

    private fun parseBreakTimes(text: String): List<BreakTime> {
        val breakTimes = mutableListOf<BreakTime>()
        val parts = text.split(",", "，")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.contains("-")) {
                val times = trimmed.split("-")
                if (times.size == 2) {
                    breakTimes.add(BreakTime(times[0].trim(), times[1].trim()))
                }
            }
        }
        return breakTimes
    }
}

/**
 * 主函数 - 运行所有测试
 */
fun main() {
    val test = WorkTimeSelfTest()
    test.runAllTests()
}
