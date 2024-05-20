@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)

package com.wilinz.devtools

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsControllerCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.wilinz.accessbilityx.app.launchAppPackage
import com.wilinz.devtools.service.AutoAccessibilityService
import com.wilinz.devtools.service.FloatingWindowService
import com.wilinz.devtools.ui.theme.DevtoolsTheme
import com.wilinz.devtools.util.PermissionsSettingUtil
import com.wilinz.devtools.util.isAccessibilityServiceEnabled
import com.wilinz.devtools.util.toast
import java.security.AccessController.getContext


class MainActivity : ComponentActivity() {

    val auto get() = AutoAccessibilityService.instance

    val ipv4Regex = Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}:\d+$""")

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        setContent {
            DevtoolsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        topBar = {
                            SmallTopAppBar(title = { Text(text = stringResource(id = R.string.app_name)) })
                        }
                    ) {
                        Column(
                            Modifier
                                .padding(it)
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Column(Modifier.padding(16.dp)) {
                                Text(text = """必须打开通知权限，无障碍权限，悬浮窗权限才能运行，非必要不要相信 Ai 的答案，题库找不到的情况下才可以参考
                                    |
                                    |悬浮窗按钮说明：
                                    |1. 第一个按钮：点击可开始查找答案
                                    |2. 第二个按钮：点击可切换答案的显示与否
                                    |3. 第三个按钮：点击可切换窗口显示与否，拖动可移动窗口
                                    |
                                    |使用方法：
                                    |打开所有权限后，点击“启动”，进入微信后，手动进入答题页面，点击“第一个按钮”，即可获取答案，翻页后可重新点击
                                    |
                                """.trimMargin())


                                ElevatedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                    startActivity(
                                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK
                                        )
                                    )
                                }) {
                                    Text(text = "无障碍权限（请勾选 FuckKetangpai）")
                                }

                                val scope = rememberCoroutineScope()
                                val context = LocalContext.current

                                val notificationPermission =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS) { ok ->
                                            if (ok) {
                                                toast("已授权")
                                            }
                                        }
                                    } else {
                                        null
                                    }
                                ElevatedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                    notificationPermission?.launchPermissionRequest() ?: run {
                                        toast("已授权")
                                    }
                                }) {
                                    Text(text = "打开通知权限")
                                }

                                ElevatedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(
                                            this@MainActivity
                                        )
                                    ) {
                                        startCompatibleForegroundService(
                                            this@MainActivity,
                                            Intent(
                                                this@MainActivity,
                                                FloatingWindowService::class.java
                                            )
                                        )
                                    } else {
                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                        startActivity(intent)
                                    }
                                }) {
                                    Text(text = "打开悬浮窗")
                                }

                                ElevatedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        if (Settings.canDrawOverlays(context)){
                                            toast("你已经有悬浮窗权限")
                                        }
                                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                                    }else{
                                        toast("你已经有悬浮窗权限")
                                    }
                                }) {
                                    Text(text = "悬浮窗权限设置")
                                }

                                ElevatedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                    if (checkEnv(context)) launchAppPackage("com.tencent.mm")
                                }) {
                                    Text(text = "启动！！！")
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                Text(text = "好用请点击下方链接给个 star 吧！")
                                LinkText(text = "Power By Wilinz: https://github.com/wilinz/FuckKetangpai", url = "https://github.com/wilinz/FuckKetangpai")
                                Text(text = "后续如果出现无法使用请前往 Github 下载最新版本")
                            }
                        }
                    }

                }
            }
        }
    }

    @Composable
    fun LinkText(text: String, url: String) {
        val context = LocalContext.current
        val annotatedString = AnnotatedString.Builder(text).apply {
            addStyle(
                style = SpanStyle(
                    textDecoration = TextDecoration.Underline
                ),
                start = 0,
                end = text.length
            )
        }.toAnnotatedString()

        Text(
            text = annotatedString,
            modifier = Modifier.clickable {
                // 使用CustomTabsIntent打开链接
//                val customTabsIntent = CustomTabsIntent.Builder().build()
//                customTabsIntent.launchUrl(context, url.toUri())
                // 使用标准的Intent打开链接
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
        )
    }

    fun startCompatibleForegroundService(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun checkEnv(context: Context): Boolean {
        val acc = isAccessibilityServiceEnabled(context, AutoAccessibilityService::class.java)
        val overlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
        } else true
        if (!acc) {
            toast("未授权无障碍权限")
        }
        if (!overlay) {
            toast("未授权悬浮窗权限")
        }
        if (!notification) {
            toast("未授权通知权限")
        }
        return acc && overlay && notification
    }

    private fun checkPermission(context: Context, permission: String): Boolean {
        val res: Int = context.checkCallingOrSelfPermission(permission)
        return res == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DevtoolsTheme {
        Greeting("Android")
    }
}

const val TAG = "MainActivity.kt"