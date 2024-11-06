package com.wilinz.fuckketangpai.util

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.wilinz.fuckketangpai.BuildConfig
import java.io.BufferedReader
import java.io.IOException

object PermissionsSettingUtil {
    /**
     * Build.MANUFACTURER判断各大手机厂商品牌
     */
    private const val MANUFACTURER_HUAWEI = "huawei" //华为
    private const val MANUFACTURER_MEIZU = "meizu" //魅族
    private const val MANUFACTURER_XIAOMI = "xiaomi" //小米
    private const val MANUFACTURER_SONY = "sony" //索尼
    private const val MANUFACTURER_OPPO = "oppo"
    private const val MANUFACTURER_LG = "lg"
    private const val MANUFACTURER_LETV = "letv" //乐视

    /**
     * 跳转到相应品牌手机系统权限设置页，如果跳转不成功，则跳转到应用详情页
     * 这里需要改造成返回true或者false，应用详情页:true，应用权限页:false
     */
    fun getAppPermissionsSettingIntent(): Intent {
        return when (Build.MANUFACTURER.lowercase()) {
            MANUFACTURER_HUAWEI -> huawei()
            MANUFACTURER_MEIZU -> meizu()
            MANUFACTURER_XIAOMI -> xiaomi()
            MANUFACTURER_SONY -> sony()
            MANUFACTURER_OPPO -> oppo()
            MANUFACTURER_LG -> lg()
            MANUFACTURER_LETV -> letv()
            else -> getAppDetailSettingIntent()
        }
    }

    /**
     * 华为跳转权限设置页
     */
    private fun huawei(): Intent {
        val intent = Intent()
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("packageName", BuildConfig.APPLICATION_ID)
        val comp = ComponentName(
            "com.huawei.systemmanager",
            "com.huawei.permissionmanager.ui.MainActivity"
        )
        intent.component = comp
        return intent
    }

    /**
     * 魅族跳转权限设置页，测试时，点击无反应，具体原因不明
     */
    private fun meizu(): Intent {
        val intent = Intent("com.meizu.safe.security.SHOW_APPSEC")
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.putExtra("packageName", BuildConfig.APPLICATION_ID)
        return intent
    }

    private fun xiaomi(): Intent {
        val intent = Intent("miui.intent.action.APP_PERM_EDITOR").putExtra(
            "extra_pkgname",
            BuildConfig.APPLICATION_ID
        )
        getMIUIVersion()?.let {
            val versionCode = it.trimStart('V').toIntOrNull()
            versionCode?.let {
                Log.d("version: ",it.toString())
                return intent.setClassName(
                    "com.miui.securitycenter",
                    if (it >= 8)  "com.miui.permcenter.permissions.PermissionsEditorActivity"
                    else "com.miui.permcenter.permissions.AppPermissionsEditorActivity"
                )
            }
        }
        return intent.setClassName(
            "com.miui.securitycenter",
            "com.miui.permcenter.permissions.PermissionsEditorActivity"
        )
    }

    /**
     * 索尼，6.0以上的手机非常少，基本没看见
     */
    private fun sony(): Intent {
        val intent = Intent()
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("packageName", BuildConfig.APPLICATION_ID)
        val comp = ComponentName("com.sonymobile.cta", "com.sonymobile.cta.SomcCTAMainActivity")
        intent.component = comp
        return intent
    }

    /**
     * OPPO
     */
    private fun oppo(): Intent {
        val intent = Intent()
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("packageName", BuildConfig.APPLICATION_ID)
        val comp = ComponentName(
            "com.color.safecenter",
            "com.color.safecenter.permission.PermissionManagerActivity"
        )
        intent.component = comp
        return intent
    }

    /**
     * LG经过测试，正常使用
     */
    private fun lg(): Intent {
        val intent = Intent("android.intent.action.MAIN")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("packageName", BuildConfig.APPLICATION_ID)
        val comp = ComponentName(
            "com.android.settings",
            "com.android.settings.Settings\$AccessLockSummaryActivity"
        )
        intent.component = comp
        return intent
    }

    /**
     * 乐视6.0以上很少，基本都可以忽略了，现在乐视手机不多
     */
    private fun letv(): Intent {
        val intent = Intent()
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("packageName", BuildConfig.APPLICATION_ID)
        val comp = ComponentName(
            "com.letv.android.letvsafe",
            "com.letv.android.letvsafe.PermissionAndApps"
        )
        intent.component = comp
        return intent
    }

    /**
     * 只能打开到自带安全软件
     */
    private fun `360`(): Intent {
        val intent = Intent("android.intent.action.MAIN")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("packageName", BuildConfig.APPLICATION_ID)
        val comp = ComponentName(
            "com.qihoo360.mobilesafe",
            "com.qihoo360.mobilesafe.ui.index.AppEnterActivity"
        )
        intent.component = comp
        return intent
    }

    /**
     * 获取应用详情页面
     */
    fun getAppDetailSettingIntent(): Intent {
        val localIntent = Intent()
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        localIntent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
        localIntent.data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
        return localIntent
    }

    private fun getMIUIVersion(): String? {
        return getSystemProperty("ro.miui.ui.version.name")
    }

    private fun getSystemProperty(propName: String): String? {
        var input: BufferedReader? = null
        return try {
            val p = Runtime.getRuntime().exec("getprop $propName")
            input = p.inputStream.bufferedReader()
            input.readLine()
        } catch (ex: IOException) {
            ex.printStackTrace()
            null
        } finally {
            try {
                input?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

}