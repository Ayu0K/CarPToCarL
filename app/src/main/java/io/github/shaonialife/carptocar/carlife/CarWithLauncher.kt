package io.github.shaonialife.carptocar.carlife

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * 自动拉起小米 CarWith，让它进入 CarLife 连接流程 —— 移植自 mcarplay 的
 * `com.mcarplay.app.carlife.CarLifeLauncher`（只取「公开入口」路径，丢掉其 root/Shizuku 回退
 * 与诊断设施，本仓库没有这些依赖）。
 *
 * 核心只有一条显式 [ComponentName]：CarWith 4.x 的
 * `com.miui.carlink.castfwk.usb.CarlifeAdbActivity` 是**已导出的普通 Activity**，用普通应用
 * Intent 即可启动（类名带 Adb 只是历史命名，不需要 ADB / root / Shizuku）。其 `onCreate` 会
 * 自己以 `startType=3` 拉起内部 CarlinkService → CarlifeConnectService，最终让 CarWith 在
 * `127.0.0.1` 监听 CarLife 七口（7240/8240/...），届时本项目的 [CarLifeHijack] 守候线程即可
 * 照常接管画面与音频。
 *
 * **重要语义**：[EntryLaunchResult.REQUESTED] 只说明 `startActivity` 没有抛异常，**不能证明**
 * CarWith 真的收到请求或成功建链。MIUI「后台弹出界面」被拒时 `startActivity` 会静默失败且
 * 不抛异常；CarWith 自己还有开关 / 隐私同意 / 已有会话等外部不可见的静默门槛。因此调用方
 * 不要把这个返回码当验收，真正成功的判据是 7240 端口随后开始监听。
 */
object CarWithLauncher {
    const val CARWITH_PACKAGE = "com.miui.carlink"
    const val CARWITH_ENTRY = "com.miui.carlink.castfwk.usb.CarlifeAdbActivity"
    const val CARWITH_SETTINGS = "com.carwith.launcher.settings.phone.UCarScreenSettingsActivity"
    const val CARLIFE_PACKAGE = "com.baidu.carlife.xiaomi"

    enum class EntryLaunchResult {
        REQUESTED,
        MISSING,
        DISABLED,
        NOT_EXPORTED,
        PERMISSION_DENIED,
        FAILED,
    }

    /** CarWith 与百度 CarLife 引擎是否都已安装（包可见性依赖 Manifest 的 queries 声明）。 */
    fun isAvailable(context: Context): Boolean =
        runCatching {
            context.packageManager.getApplicationInfo(CARWITH_PACKAGE, 0)
            context.packageManager.getApplicationInfo(CARLIFE_PACKAGE, 0)
            true
        }.getOrDefault(false)

    /**
     * 请求 CarWith 公开入口（自动进入 CarLife 无线连接）。
     * 结果分级便于界面提示；[REQUESTED] 不等于建链成功，见类注释。
     */
    fun requestPublicEntry(context: Context): EntryLaunchResult =
        startExportedActivity(context, CARWITH_PACKAGE, CARWITH_ENTRY)

    /**
     * 公开入口不可用时的回退：打开 CarWith 设置页，让用户在原厂 UI 里完成「CarLife 无线连接」。
     * 用户走通原厂路径后，就绪的七口会被 [CarLifeHijack] 直接附着。
     */
    fun requestSettings(context: Context): EntryLaunchResult =
        startExportedActivity(context, CARWITH_PACKAGE, CARWITH_SETTINGS)

    private fun startExportedActivity(
        context: Context,
        packageName: String,
        className: String,
    ): EntryLaunchResult =
        try {
            val component = ComponentName(packageName, className)
            val info = context.packageManager.getActivityInfo(component, 0)
            when {
                !info.enabled -> EntryLaunchResult.DISABLED
                !info.exported -> EntryLaunchResult.NOT_EXPORTED
                else -> {
                    context.startActivity(
                        Intent().setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                    EntryLaunchResult.REQUESTED
                }
            }
        } catch (error: Exception) {
            when (error) {
                is PackageManager.NameNotFoundException,
                is ActivityNotFoundException -> EntryLaunchResult.MISSING
                is SecurityException -> EntryLaunchResult.PERMISSION_DENIED
                else -> EntryLaunchResult.FAILED
            }
        }

    /** 结果转人类可读提示，供日志 / 界面复用。 */
    fun describe(result: EntryLaunchResult): String =
        when (result) {
            EntryLaunchResult.REQUESTED -> "已发出请求（是否真正建链以 7240 端口为准）"
            EntryLaunchResult.MISSING -> "CarWith / CarLife 组件不存在（版本过低或未安装）"
            EntryLaunchResult.DISABLED -> "入口组件被禁用"
            EntryLaunchResult.NOT_EXPORTED -> "入口组件未导出，无法从外部启动"
            EntryLaunchResult.PERMISSION_DENIED -> "被系统拒绝（包可见性或后台启动权限）"
            EntryLaunchResult.FAILED -> "启动失败（未知异常）"
        }
}
