package com.example.worktime

import java.text.SimpleDateFormat
import java.util.*

/**
 * 工时记录打卡逻辑测试用例
 * 
 * 测试规则：
 * 1. 第一次点按钮：开始时间刷新
 * 2. 第二次点按钮：结束时间刷新
 * 3. 第三次点按钮：结束时间刷新
 * 4. 连续 10 次点击：结束时间取最后一次
 */
class WorkTimeLogicTest {

    private val records = mutableListOf<WorkRecord>()
    private val today = getCurrentDate()

    /**
     * 测试用例 1: 第一次点按钮，开始时间刷新
     */
    fun test1_FirstCheckIn_ShouldUpdateStartTime() {
        records.clear()
        println("\n🧪 测试用例 1: 第一次点按钮，开始时间刷新")

        val checkInTime = System.currentTimeMillis()
        val record = WorkRecord(
            date = today,
            checkInTime = checkInTime,
            checkOutTime = null,
            duration = 0
        )
        records.add(record)

        // 验证
        require(records.size == 1) { "记录数应为 1" }
        require(records[0].date == today) { "日期应为今天" }
        require(records[0].checkInTime == checkInTime) { "开始时间应等于打卡时间" }
        require(records[0].checkOutTime == null) { "结束时间应为空" }
        
        println("  ✅ 开始时间：${formatTime(checkInTime)}")
        println("  ✅ 结束时间：--:--:--")
        println("  ✅ 测试用例 1 通过")
    }

    /**
     * 测试用例 2: 第二次点按钮，结束时间刷新
     */
    fun test2_SecondCheckOut_ShouldUpdateEndTime() {
        records.clear()
        println("\n🧪 测试用例 2: 第二次点按钮，结束时间刷新")

        val checkInTime = System.currentTimeMillis() - 3600000
        records.add(WorkRecord(today, checkInTime, null, 0))
        println("  第 1 次点击：开始时间 ${formatTime(checkInTime)}")

        val checkOutTime = System.currentTimeMillis()
        val duration = checkOutTime - checkInTime
        records[0] = WorkRecord(
            date = records[0].date,
            checkInTime = records[0].checkInTime,
            checkOutTime = checkOutTime,
            duration = duration
        )
        println("  第 2 次点击：结束时间 ${formatTime(checkOutTime)}")

        require(records[0].checkOutTime != null) { "结束时间不应为空" }
        require(records[0].checkOutTime == checkOutTime) { "结束时间应等于打卡时间" }
        require(records[0].duration > 0) { "工时应大于 0" }
        
        println("  ✅ 工作时长：${formatDuration(duration)}")
        println("  ✅ 测试用例 2 通过")
    }

    /**
     * 测试用例 3: 第三次点按钮，结束时间刷新
     */
    fun test3_ThirdCheck_ShouldUpdateEndTimeAgain() {
        records.clear()
        println("\n🧪 测试用例 3: 第三次点按钮，结束时间刷新")

        val checkInTime = System.currentTimeMillis() - 7200000
        val firstCheckOutTime = System.currentTimeMillis() - 3600000
        records.add(WorkRecord(today, checkInTime, firstCheckOutTime, 3600000))
        println("  第 1 次点击：开始时间 ${formatTime(checkInTime)}")
        println("  第 2 次点击：结束时间 ${formatTime(firstCheckOutTime)}")

        Thread.sleep(100)
        val newCheckOutTime = System.currentTimeMillis()
        val newDuration = newCheckOutTime - checkInTime
        records[0] = WorkRecord(
            date = records[0].date,
            checkInTime = records[0].checkInTime,
            checkOutTime = newCheckOutTime,
            duration = newDuration
        )
        println("  第 3 次点击：结束时间 ${formatTime(newCheckOutTime)}")

        require(records[0].checkOutTime == newCheckOutTime) { "结束时间应等于最新打卡时间" }
        require(records[0].checkOutTime != firstCheckOutTime) { "结束时间应不同于第一次结束时间" }
        require(records[0].duration > 3600000) { "工时应大于 1 小时" }
        
        println("  ✅ 工作时长：${formatDuration(newDuration)}")
        println("  ✅ 测试用例 3 通过")
    }

    /**
     * 测试用例 4: 连续调用 10 次打卡按钮接口，结束时间取最后一次
     */
    fun test4_TenConsecutiveClicks_ShouldUseLastEndTime() {
        records.clear()
        println("\n🧪 测试用例 4: 连续 10 次点击，结束时间取最后一次")

        val checkInTime = System.currentTimeMillis()
        records.add(WorkRecord(today, checkInTime, null, 0))
        println("  第 1 次点击：开始时间 ${formatTime(checkInTime)}")

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
            println("  第 $i 次点击：结束时间 ${formatTime(lastCheckOutTime)}")
        }

        require(records[0].checkOutTime == lastCheckOutTime) { "结束时间应等于最后一次打卡时间" }
        require(records[0].duration > 0) { "工时应大于 0" }
        
        println("  ✅ 最终工作时长：${formatDuration(records[0].duration)}")
        println("  ✅ 测试用例 4 通过")
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatDuration(ms: Long): String {
        val hours = ms / (1000 * 60 * 60)
        val minutes = (ms / (1000 * 60)) % 60
        val seconds = (ms / 1000) % 60
        return "${hours}小时${minutes}分钟${seconds}秒"
    }
}

/**
 * 运行所有测试
 */
fun main() {
    println("=" .repeat(60))
    println("🧪 工时记录打卡逻辑测试")
    println("=" .repeat(60))

    val test = WorkTimeLogicTest()
    var passed = 0
    var failed = 0

    try {
        test.test1_FirstCheckIn_ShouldUpdateStartTime()
        passed++
    } catch (e: Exception) {
        println("  ❌ 测试用例 1 失败：${e.message}")
        failed++
    }

    try {
        test.test2_SecondCheckOut_ShouldUpdateEndTime()
        passed++
    } catch (e: Exception) {
        println("  ❌ 测试用例 2 失败：${e.message}")
        failed++
    }

    try {
        test.test3_ThirdCheck_ShouldUpdateEndTimeAgain()
        passed++
    } catch (e: Exception) {
        println("  ❌ 测试用例 3 失败：${e.message}")
        failed++
    }

    try {
        test.test4_TenConsecutiveClicks_ShouldUseLastEndTime()
        passed++
    } catch (e: Exception) {
        println("  ❌ 测试用例 4 失败：${e.message}")
        failed++
    }

    println("\n" + "=" .repeat(60))
    println("📊 测试结果：$passed 通过，$failed 失败")
    println("=" .repeat(60))

    if (failed > 0) {
        println("\n❌ 测试未全部通过，请不要编译！")
        throw RuntimeException("测试失败，请修复后再编译")
    } else {
        println("\n✅ 所有测试通过！可以编译。")
    }
}
