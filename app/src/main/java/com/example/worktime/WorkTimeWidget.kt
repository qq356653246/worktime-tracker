package com.example.worktime

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.*

/**
 * 桌面打卡组件
 */
class WorkTimeWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_CHECK -> performCheck(context)
            ACTION_OPEN_APP -> openApp(context)
        }
    }

    private fun openApp(context: Context) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent != null) {
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun performCheck(context: Context) {
        try {
            val prefs = context.getSharedPreferences("worktime_data", Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val today = getCurrentDate()
            
            // 重新加载记录（确保最新数据）
            val records = loadRecords(prefs).toMutableList()
            val todayIndex = records.indexOfFirst { it.date == today }
            
            if (todayIndex == -1) {
                // 今天还没有打卡，执行签到
                checkIn(context, today, now, records, prefs)
            } else {
                // 今天已经打过卡，检查是否已签退
                val todayRecord = records[todayIndex]
                if (todayRecord.checkOutTime == null) {
                    // 已签到但未签退，执行签退
                    checkOut(context, todayIndex, now, records, prefs)
                } else {
                    // 今天已完成打卡 - 允许重新开始（覆盖今天的记录）
                    checkIn(context, today, now, records, prefs)
                }
            }
            
            // 强制更新所有 widget
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, WorkTimeWidget::class.java)
            )
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
            
            // 发送广播通知 MainActivity 刷新
            val refreshIntent = android.content.Intent("com.example.worktime.REFRESH_WIDGET")
            context.sendBroadcast(refreshIntent)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkIn(context: Context, date: String, time: Long, records: MutableList<WorkRecord>, prefs: SharedPreferences) {
        val record = WorkRecord(
            date = date,
            checkInTime = time,
            checkOutTime = null,
            duration = 0
        )
        records.add(0, record)
        saveRecords(records, prefs)
        showToast(context, "✅ 开始打卡")
    }

    private fun checkOut(context: Context, index: Int, time: Long, records: MutableList<WorkRecord>, prefs: SharedPreferences) {
        val oldRecord = records[index]
        val rawDuration = time - oldRecord.checkInTime
        
        // 计算休息时间
        val breakTimes = loadBreakTimes(prefs)
        var totalBreakMs = 0L
        val calendar = Calendar.getInstance()
        
        for (breakTime in breakTimes) {
            val (breakStartHour, breakStartMin) = breakTime.startTime.split(":").map { it.toInt() }
            val (breakEndHour, breakEndMin) = breakTime.endTime.split(":").map { it.toInt() }
            
            calendar.time = Date(oldRecord.checkInTime)
            calendar.set(Calendar.HOUR_OF_DAY, breakStartHour)
            calendar.set(Calendar.MINUTE, breakStartMin)
            calendar.set(Calendar.SECOND, 0)
            val breakStartMs = calendar.timeInMillis
            
            calendar.time = Date(oldRecord.checkInTime)
            calendar.set(Calendar.HOUR_OF_DAY, breakEndHour)
            calendar.set(Calendar.MINUTE, breakEndMin)
            calendar.set(Calendar.SECOND, 0)
            val breakEndMs = calendar.timeInMillis
            
            val actualStart = maxOf(breakStartMs, oldRecord.checkInTime)
            val actualEnd = minOf(breakEndMs, time)
            
            if (actualEnd > actualStart) {
                totalBreakMs += (actualEnd - actualStart)
            }
        }
        
        val netDuration = rawDuration - totalBreakMs
        
        records[index] = WorkRecord(
            date = oldRecord.date,
            checkInTime = oldRecord.checkInTime,
            checkOutTime = time,
            duration = netDuration
        )
        
        saveRecords(records, prefs)
        
        val hours = netDuration / (1000 * 60 * 60)
        val minutes = (netDuration / (1000 * 60)) % 60
        
        showToast(context, "✅ 结束打卡\n工作时长：${hours}小时${minutes}分钟")
    }

    private fun showToast(context: Context, message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
    }

    private fun loadRecords(prefs: SharedPreferences): MutableList<WorkRecord> {
        val json = prefs.getString("records", null)
        if (json != null) {
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<MutableList<WorkRecord>>() {}.type
            return gson.fromJson(json, type)
        }
        return mutableListOf()
    }

    private fun saveRecords(records: MutableList<WorkRecord>, prefs: SharedPreferences) {
        val editor = prefs.edit()
        val gson = com.google.gson.Gson()
        val json = gson.toJson(records)
        editor.putString("records", json)
        editor.apply()
    }

    private fun loadBreakTimes(prefs: SharedPreferences): MutableList<BreakTime> {
        val json = prefs.getString("breakTimes", null)
        if (json != null) {
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<MutableList<BreakTime>>() {}.type
            return gson.fromJson(json, type)
        }
        return mutableListOf(BreakTime("12:00", "13:00"))
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    companion object {
        const val ACTION_CHECK = "com.example.worktime.ACTION_CHECK"
        const val ACTION_OPEN_APP = "com.example.worktime.ACTION_OPEN_APP"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val prefs = context.getSharedPreferences("worktime_data", Context.MODE_PRIVATE)
                val views = RemoteViews(context.packageName, R.layout.widget_layout)
                
                // 设置日期
                val sdfDate = SimpleDateFormat("MM 月 dd 日", Locale.CHINA)
                views.setTextViewText(R.id.widgetDate, sdfDate.format(Date()))
                
                // 获取今日状态
                val today = getTodayDate()
                val records = getRecords(prefs)
                val todayIndex = records.indexOfFirst { it.date == today }
                
                if (todayIndex == -1) {
                    // 未打卡
                    views.setTextViewText(R.id.widgetCheckButton, "打卡")
                    views.setTextViewText(R.id.widgetStatus, "点击开始工作")
                    views.setViewVisibility(R.id.widgetDuration, android.view.View.GONE)
                } else {
                    val todayRecord = records[todayIndex]
                    if (todayRecord.checkOutTime == null) {
                        // 已签到未签退
                        views.setTextViewText(R.id.widgetCheckButton, "下班")
                        views.setTextViewText(R.id.widgetStatus, "工作中...")
                        
                        // 显示已工作时长
                        val elapsed = System.currentTimeMillis() - todayRecord.checkInTime
                        val hours = elapsed / (1000 * 60 * 60)
                        val minutes = (elapsed / (1000 * 60)) % 60
                        views.setTextViewText(R.id.widgetDuration, "已工作：${hours}小时${minutes}分钟")
                        views.setViewVisibility(R.id.widgetDuration, android.view.View.VISIBLE)
                    } else {
                        // 已完成 - 可以重新打卡
                        views.setTextViewText(R.id.widgetCheckButton, "重新打卡")
                        views.setTextViewText(R.id.widgetStatus, "今日已完成，点击重新开始")
                        
                        val hours = todayRecord.duration / (1000 * 60 * 60)
                        val minutes = (todayRecord.duration / (1000 * 60)) % 60
                        views.setTextViewText(R.id.widgetDuration, "上次时长：${hours}小时${minutes}分钟")
                        views.setViewVisibility(R.id.widgetDuration, android.view.View.VISIBLE)
                    }
                }
                
                // 设置打卡按钮点击事件
                val checkIntent = Intent(context, WorkTimeWidget::class.java)
                checkIntent.action = ACTION_CHECK
                val checkPendingIntent = PendingIntent.getBroadcast(
                    context, 
                    0, 
                    checkIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widgetCheckButton, checkPendingIntent)
                
                // 设置打开 App 按钮点击事件
                val openAppIntent = Intent(context, WorkTimeWidget::class.java)
                openAppIntent.action = ACTION_OPEN_APP
                val openAppPendingIntent = PendingIntent.getBroadcast(
                    context,
                    1,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widgetOpenAppButton, openAppPendingIntent)
                
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun getRecords(prefs: SharedPreferences): MutableList<WorkRecord> {
            val json = prefs.getString("records", null)
            if (json != null) {
                val gson = com.google.gson.Gson()
                val type = object : com.google.gson.reflect.TypeToken<MutableList<WorkRecord>>() {}.type
                return gson.fromJson(json, type)
            }
            return mutableListOf()
        }

        private fun getTodayDate(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}
