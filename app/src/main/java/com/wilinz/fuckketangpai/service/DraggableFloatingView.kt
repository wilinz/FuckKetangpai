@file:OptIn(ExperimentalComposeUiApi::class)

package com.wilinz.fuckketangpai.service

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.wilinz.fuckketangpai.R
import com.wilinz.fuckketangpai.ui.theme.DevtoolsTheme
import com.wilinz.fuckketangpai.util.copyToClipboard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class DraggableFloatingView(
    private val context: Context,
    private val answerFlow: MutableStateFlow<String>,
    private val isLoadingFlow: MutableStateFlow<Boolean>
) {
    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var floatingView: ComposeView

    // 用于跟踪触摸事件的变量
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f

    var onStartCallback: (() -> Unit)? = null
    var onRemoveWindowCallback: (() -> Unit)? = null
    var isRemoved: Boolean = false

    private fun setLifecycleOwner(
        lifecycleOwner: MySavedStateRegistryOwner,
        viewModelStoreOwner: ViewModelStoreOwner
    ) {
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        floatingView.setViewTreeLifecycleOwner(lifecycleOwner)
        floatingView.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
        floatingView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
    }

    fun create(
        lifecycleOwner: MySavedStateRegistryOwner,
        viewModelStoreOwner: ViewModelStoreOwner,
        isFirst: Boolean = true
    ) {

        layoutParams = WindowManager.LayoutParams().apply {
            y = 300
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.START or Gravity.START
        }

        floatingView = ComposeView(context)

        setContent()
        setLifecycleOwner(lifecycleOwner, viewModelStoreOwner)
        windowManager.addView(floatingView, layoutParams)

    }

    @Composable
    fun CircularIconButton(
        modifier: Modifier = Modifier,
        icon: ImageVector,
        iconDescription: String? = null,
        iconColor: Color = Color.White,
        backgroundColor: Color = Color.Gray,
        interactionSource: MutableInteractionSource // 添加这个参数
    ) {
        // 使用 rememberRipple 创建水波纹效果
//        val ripple = rememberRipple(bounded = true, color = MaterialTheme.colorScheme.onSurface)

        Surface(
            modifier = modifier
                .padding(8.dp)
                .size(32.dp)
                .clip(CircleShape), // 确保 Surface 本身是圆形的
//                .indication(interactionSource, ripple), // 使用 indication 和 interactionSource
            color = backgroundColor,
            shape = CircleShape
        ) {
            IconButton(onClick = { /* 这里不处理点击事件 */ }) {
                Icon(
                    painter = painterResource(id = R.drawable.drag),
                    contentDescription = iconDescription,
                    tint = iconColor
                )
            }
        }
    }

    fun setContent() {

        val threshold =
            ViewConfiguration.get(context).scaledTouchSlop // Threshold for considering a touch as a drag

        floatingView.setContent {
            DevtoolsTheme {
                var isHide by remember {
                    mutableStateOf(false)
                }
                var minimization by remember {
                    mutableStateOf(false)
                }
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(if (isHide || minimization) 0x05000000 else 0x50000000 ))

                ) {

                    var tip by remember {
                        mutableStateOf("")
                    }

                    Column {
                        val isLoading by isLoadingFlow.collectAsState()


                        Row(
                            modifier = Modifier
                                .wrapContentSize(),
                            //                            .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            if (!isHide) {
                                IconButton(onClick = {
                                    if (!isLoading) onStartCallback?.invoke()
                                }) {
                                    Icon(
                                        if (isLoading) Icons.Default.Sync else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                                //                        Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = { minimization = !minimization }) {
                                    Icon(
                                        painterResource(id = if (minimization) R.drawable.maximization else R.drawable.minimization),
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }


                            DragTheButton(threshold, onClick = {
                                isHide = !isHide
                            })
                        }
                        val answer by answerFlow.collectAsState()
                        val context = LocalContext.current

                        if (!isHide && !minimization && (answer.isNotBlank() || isLoading || tip.isNotBlank())) {
                            Column(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .padding(8.dp, 0.dp, 8.dp, 8.dp),
                            ) {
                                if (isLoading) {
                                    Text(text = "正在获取...", color = Color.White)
                                }
                                if (tip.isNotBlank()) {
                                    Text(text = tip, color = Color.White)
                                }
                                if (answer.isNotBlank()) {
                                    Text(text = answer, color = Color.White, modifier = Modifier.clickable {
                                        copyToClipboard(context, answer)
                                    })
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    @Composable
    private fun DragTheButton(threshold: Int, onClick: () -> Unit) {
        var isDrag by remember {
            mutableStateOf(false)
        }  // Add this variable to track if the view was dragged
        val interactionSource = remember { MutableInteractionSource() }
        val coroutineScope = rememberCoroutineScope()

        Box(
            modifier = Modifier
                .wrapContentSize()
                .pointerInteropFilter { motionEvent ->
                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> {
                            isDrag = false // Reset the drag state on ACTION_DOWN
                            initialX = layoutParams.x
                            initialY = layoutParams.y
                            initialTouchX = motionEvent.rawX
                            initialTouchY = motionEvent.rawY
                            true // Handle the event
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = (motionEvent.rawX - initialTouchX).toInt()
                            val deltaY = (motionEvent.rawY - initialTouchY).toInt()
                            if (abs(deltaX) > threshold || abs(deltaY) > threshold) {
                                isDrag =
                                    true // Set isDrag to true as the view is being dragged
                                layoutParams.x = initialX + deltaX
                                layoutParams.y = initialY + deltaY
                                windowManager.updateViewLayout(floatingView, layoutParams)
                            }
                            true // Handle the event
                        }

                        MotionEvent.ACTION_UP -> {
                            if (!isDrag) {
                                onClick.invoke()
                                coroutineScope.launch {
                                    val position = Offset(motionEvent.x, motionEvent.y)
                                    val press = PressInteraction.Press(position)
                                    interactionSource.emit(press)

                                    // 延迟一段时间后，发出 Release 交互以完成水波纹效果
                                    delay(100) // 水波纹的持续时间
                                    interactionSource.emit(PressInteraction.Release(press))
                                }
                            }
                            true // Do not handle the event
                        }

                        else -> false // Do not handle the event
                    }
                }
        ) {
            CircularIconButton(
                icon = Icons.Default.PlayArrow,
                backgroundColor = Color(0x05000000),
                interactionSource = interactionSource // 传递 interactionSource
            )
        }
    }


    fun remove() {
        windowManager.removeView(floatingView)
        isRemoved = true
    }

    private val auto get() = AutoAccessibilityService.instance

    private fun collectTextFromNode(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return

        // If the node has text and it is not empty, add it to the list
        node.text?.let {
            if (it.isNotBlank()) {
                list.add(it.toString().trim())
            }
        }

        // Recursively call this function for all child nodes
        for (i in 0 until node.childCount) {
            collectTextFromNode(node.getChild(i), list)
        }
    }

    private fun findAllTexts(root: AccessibilityNodeInfo?): List<String> {
        val list = mutableListOf<String>()
        collectTextFromNode(root, list)
        return list
    }

}
