package com.example.worktime

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * 自动打卡无障碍服务
 */
class AutoCheckAccessibilityService : AccessibilityService() {

    companion object {
        // 公司打卡 App 包名（可配置）
        var targetPackageName: String = "com.alibaba.android.rimet" // 默认钉钉
        
        // 打卡按钮文字（可配置）
        var checkButtonTexts = listOf("上班打卡", "下班打卡", "打卡", "签到", "签退")
        
        // 是否正在执行自动打卡
        var isAutoChecking = false
        
        // 回调接口
        var onCheckResult: ((Boolean, String) -> Unit)? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                   AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        serviceInfo = info
        Toast.makeText(this, "自动打卡服务已启动", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isAutoChecking || event == null) return
        
        // 检查是否为目标 App
        val packageName = event.packageName?.toString() ?: return
        if (packageName != targetPackageName) return
        
        // 查找并点击打卡按钮
        if (findAndClickCheckButton()) {
            isAutoChecking = false
            onCheckResult?.invoke(true, "打卡成功")
            Toast.makeText(this, "✅ 自动打卡成功", Toast.LENGTH_LONG).show()
            
            // 2 秒后返回本 App
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                returnToWorkTimeApp()
            }, 2000)
        }
    }

    override fun onInterrupt() {
        isAutoChecking = false
        Toast.makeText(this, "自动打卡被中断", Toast.LENGTH_SHORT).show()
    }

    /**
     * 查找并点击打卡按钮
     */
    private fun findAndClickCheckButton(): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        
        for (buttonText in checkButtonTexts) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(buttonText)
            if (nodes.isNotEmpty()) {
                for (node in nodes) {
                    if (node.isClickable) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return true
                    }
                }
            }
        }
        
        // 尝试查找包含"打卡"文字的可点击节点
        val allNodes = ArrayList<AccessibilityNodeInfo>()
        collectAllNodes(rootNode, allNodes)
        
        for (node in allNodes) {
            val text = node.text?.toString() ?: continue
            if (text.contains("打卡") && node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }
        
        return false
    }

    /**
     * 递归收集所有节点
     */
    private fun collectAllNodes(node: AccessibilityNodeInfo, result: ArrayList<AccessibilityNodeInfo>) {
        result.add(node)
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectAllNodes(child, result)
            }
        }
    }

    /**
     * 返回本 App
     */
    private fun returnToWorkTimeApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent != null) {
            startActivity(intent)
        }
    }

    /**
     * 启动公司打卡 App
     */
    fun launchTargetApp(context: android.content.Context, packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent != null) {
                context.startActivity(intent)
                isAutoChecking = true
                Toast.makeText(context, "正在打开打卡软件...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "未找到打卡软件，请检查包名", Toast.LENGTH_LONG).show()
                isAutoChecking = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "启动失败：${e.message}", Toast.LENGTH_SHORT).show()
            isAutoChecking = false
        }
    }
}
