# 工时记录 (WorkTime Tracker)

一个简洁美观的安卓工时打卡应用，使用 Room 数据库持久化存储。

## 功能特性

- ⏱️ **圆形打卡按钮**：大尺寸圆形按钮，视觉清晰
- 📅 **日期显示**：显示当前日期（含星期）
- 📝 **打卡记录**：
  - 开始打卡时间
  - 结束打卡时间
  - 自动计算工作时长
- 💾 **Room 数据库**：SQLite 持久化存储，数据不丢失
- 📊 **历史记录**：滚动查看所有打卡记录
- 🔄 **状态恢复**：重启应用后自动恢复未完成的打卡

## 界面预览

```
┌─────────────────────────┐
│       工时记录          │
│   2026 年 03 月 11 日 星期三  │
├─────────────────────────┤
│   ┌─────────────────┐   │
│   │   开始打卡      │   │
│   │    09:00:00     │   │
│   └─────────────────┘   │
│                         │
│      ┌─────────┐        │
│      │   ✓     │  ← 大  │
│      │  圆形   │    圆  │
│      │  按钮   │    形  │
│      └─────────┘        │
│   点击打卡开始工作      │
│                         │
│   ┌─────────────────┐   │
│   │   结束打卡      │   │
│   │    18:00:00     │   │
│   └─────────────────┘   │
│                         │
│   工作时长：09:00:00    │
│                         │
│   ─── 历史记录 ───      │
│   2026-03-10  09:00-18:00  9 小时  │
│   2026-03-09  09:15-18:30  9 小时  │
└─────────────────────────┘
```

## 技术栈

- **语言**: Kotlin
- **架构**: MVVM + LiveData/Flow
- **数据库**: Room (SQLite)
- **UI**: Material Design 3
- **异步**: Kotlin Coroutines

## 数据库设计

### attendance_records 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK) | 自增主键 |
| date | String | 日期 (yyyy-MM-dd) |
| checkInTime | Long | 打卡开始时间戳 |
| checkOutTime | Long? | 打卡结束时间戳 (可空) |
| duration | Long | 工作时长 (毫秒) |

## 构建步骤

1. 用 Android Studio 打开项目
2. 等待 Gradle 同步完成
3. 连接安卓设备或启动模拟器
4. 点击 Run 按钮

## 项目结构

```
app/src/main/java/com/example/worktime/
├── MainActivity.kt           # 主界面 + 业务逻辑
├── AttendanceRecord.kt       # 数据实体
├── AttendanceDao.kt          # 数据访问对象
├── AttendanceDatabase.kt     # Room 数据库
└── HistoryAdapter.kt         # 历史记录适配器

app/src/main/res/
├── layout/
│   ├── activity_main.xml     # 主界面布局
│   └── item_history.xml      # 历史记录项布局
├── drawable/
│   ├── ic_check.xml          # 打卡图标
│   └── circle_background_glow.xml  # 圆形背景
└── values/
    ├── strings.xml
    ├── colors.xml
    └── themes.xml
```

## 后续可扩展功能

- [ ] 统计图表（周报/月报）
- [ ] 数据导出 CSV/Excel
- [ ] 工作类型分类
- [ ] 备注/标签功能
- [ ] 云同步备份
- [ ] 打卡提醒通知
- [ ] 工作日/休息日标记

## License

MIT License
