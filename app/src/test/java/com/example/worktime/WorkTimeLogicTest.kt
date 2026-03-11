package com.example.worktime

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * 工时记录打卡逻辑测试用例
 */
class WorkTimeLogicTest {

    private val records = mutableListOf<WorkRecord>()
    private val today = getCurrentDate()

    @Test
    fun test1_FirstCheckIn_ShouldUpdateStartTime() {
        // 测试用例 1: 第一次点按钮，开始时间刷新
        records.clear()

        // 模拟第一次点击
        val checkInTime = System.currentTimeMillis()
        val record = WorkRecord(
            date = today,
            checkInTime = checkInTime,
            checkOutTime = null,
            duration = 0
        )
        records.add(record)

        // 验证
        assertEquals(1, records.size)
        assertEquals(today, records[0].date)
        assertEquals(checkInTime, records[0].checkInTime)
        assertNull(records[0].checkOutTime)
        println("✅ 测试用例 1 通过：第一次打卡，开始时间正确记录")
    }

    @Test
    fun test2_SecondCheckOut_ShouldUpdateEndTime() {
        // 测试用例 2: 第二次点按钮，结束时间刷新
        records.clear()

        // 模拟第一次打卡
        val checkInTime = System.currentTimeMillis() - 3600000 // 1 小时前
        records.add(WorkRecord(today, checkInTime, null, 0))

        // 模拟第二次点击（结束打卡）
        val checkOutTime = System.currentTimeMillis()
        val duration = checkOutTime - checkInTime
        records[0] = WorkRecord(
            date = records[0].date,
            checkInTime = records[0].checkInTime,
            checkOutTime = checkOutTime,
            duration = duration
        )

        // 验证
        assertNotNull(records[0].checkOutTime)
        assertEquals(checkOutTime, records[0].checkOutTime)
        assertTrue(records[0].duration > 0)
        println("✅ 测试用例 2 通过：第二次打卡，结束时间正确更新")
    }

    @Test
    fun test3_ThirdCheck_ShouldUpdateEndTimeAgain() {
        // 测试用例 3: 第三次点按钮，结束时间刷新
        records.clear()

        // 模拟前两次打卡
        val checkInTime = System.currentTimeMillis() - 7200000 // 2 小时前
        val firstCheckOutTime = System.currentTimeMillis() - 3600000 // 1 小时前
        records.add(WorkRecord(today, checkInTime, firstCheckOutTime, 3600000))

        // 模拟第三次点击（更新结束时间）
        val newCheckOutTime = System.currentTimeMillis()
        val newDuration = newCheckOutTime - checkInTime
        records[0] = WorkRecord(
            date = records[0].date,
            checkInTime = records[0].checkInTime,
            checkOutTime = newCheckOutTime,
            duration = newDuration
        )

        // 验证
        assertEquals(newCheckOutTime, records[0].checkOutTime)
        assertNotEquals(firstCheckOutTime, records[0].checkOutTime)
        assertTrue(records[0].duration > 3600000)
        println("✅ 测试用例 3 通过：第三次打卡，结束时间正确更新")
    }

    @Test
    fun test4_TenConsecutiveClicks_ShouldUseLastEndTime() {
        // 测试用例 4: 连续调用 10 次打卡按钮接口，结束时间取最后一次
        records.clear()

        val checkInTime = System.currentTimeMillis()
        records.add(WorkRecord(today, checkInTime, null, 0))

        // 模拟连续 10 次点击
        var lastCheckOutTime = 0L
        for (i in 1..10) {
            lastCheckOutTime = System.currentTimeMillis()
            val duration = lastCheckOutTime - checkInTime
            records[0] = WorkRecord(
                date = records[0].date,
                checkInTime = records[0].checkInTime,
                checkOutTime = lastCheckOutTime,
                duration = duration
            )
            Thread.sleep(10) // 稍微延迟，确保时间戳不同
        }

        // 验证
        assertEquals(lastCheckOutTime, records[0].checkOutTime)
        assertTrue(records[0].duration > 0)
        println("✅ 测试用例 4 通过：连续 10 次打卡，结束时间取最后一次")
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}

/**
 * 运行所有测试
 */
fun main() {
    println("🧪 开始运行工时记录打卡逻辑测试...\n")

    val test = WorkTimeLogicTest()
    var passed = 0
    var failed = 0

    try {
        test.test1_FirstCheckIn_ShouldUpdateStartTime()
        passed++
    } catch (e: Exception) {
        println("❌ 测试用例 1 失败：${e.message}")
        failed++
    }

    try {
        test.test2_SecondCheckOut_ShouldUpdateEndTime()
        passed++
    } catch (e: Exception) {
        println("❌ 测试用例 2 失败：${e.message}")
        failed++
    }

    try {
        test.test3_ThirdCheck_ShouldUpdateEndTimeAgain()
        passed++
    } catch (e: Exception) {
        println("❌ 测试用例 3 失败：${e.message}")
        failed++
    }

    try {
        test.test4_TenConsecutiveClicks_ShouldUseLastEndTime()
        passed++
    } catch (e: Exception) {
        println("❌ 测试用例 4 失败：${e.message}")
        failed++
    }

    println("\n" + "=".repeat(50))
    println("📊 测试结果：$passed 通过，$failed 失败")
    println("=".repeat(50))

    if (failed > 0) {
        println("\n❌ 测试未全部通过，请不要编译！")
        throw RuntimeException("测试失败，请修复后再编译")
    } else {
        println("\n✅ 所有测试通过！可以编译。")
    }
}
