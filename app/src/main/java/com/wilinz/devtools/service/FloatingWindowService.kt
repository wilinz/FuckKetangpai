package com.wilinz.devtools.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import androidx.lifecycle.SavedStateLifecycleService
import androidx.lifecycle.lifecycleScope
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatResponseFormat
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.wilinz.devtools.data.Network
import com.wilinz.devtools.data.Question
import com.wilinz.devtools.data.YanxiQuestionBankRequest
import com.wilinz.devtools.data.moshi
import com.wilinz.devtools.data.repository.YanxiRepository
import com.wilinz.devtools.util.toast
import fromJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import toJson

class FloatingWindowService : SavedStateLifecycleService() {

    private var draggableFloatingView: DraggableFloatingView? = null
    private val auto get() = AutoAccessibilityService.instance

    private var answerFlow: MutableStateFlow<String> = MutableStateFlow("")

    private var isLoadingFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private var lastJob: Job? = null

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

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, getNotification("正在运行中", "Fucking Ketangpai"))
        newFloatWindow()
    }

    private fun newFloatWindow() {
        draggableFloatingView = DraggableFloatingView(this, answerFlow, isLoadingFlow)
        draggableFloatingView!!.onRemoveWindowCallback = {

        }
        draggableFloatingView!!.onStartCallback = {
            val root = auto?.rootInActiveWindow
            if (root != null) {
                val allTexts = findAllTexts(root)
                // Do something with the list of texts, e.g., print or store them
                Log.d("onCreate: ", allTexts.joinToString("\n"))
                lastJob?.cancel()
                lastJob = lifecycleScope.launch {
                    answerFlow.emit("")
                    runCatching {
                        isLoadingFlow.emit(true)
                        openaiHandle(allTexts.joinToString("\n"))
                    }.onFailure {
                        it.printStackTrace()
                        answerFlow.emit("出错了：${it.message}")
                        toast(this@FloatingWindowService, "出错了：${it.message}")
                    }
                    isLoadingFlow.emit(false)
                }
            }

        }
        draggableFloatingView!!.create(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    private suspend fun openaiHandle(allTexts: String) = withContext(Dispatchers.IO) {
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId("gpt-3.5-turbo"),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = getPresetContent()
                ),
                ChatMessage(
                    role = ChatRole.User,
                    content = allTexts
                )
            ),
            responseFormat = ChatResponseFormat("json_object")
        )
        val completion = Network.openAI.chatCompletion(chatCompletionRequest)
        val result =
            completion.choices.firstOrNull()?.message?.content ?: throw Exception("提取题目失败")

        Log.d("openaiHandle: ", result)

        val question = moshi.fromJson<Question>(result) ?: throw Exception("提取题目失败")
        val yanxi = YanxiRepository.get(
            YanxiQuestionBankRequest(
                token = "585f14c167d24e649748da25ac8d51e6",
                title = question.question,
                options = question.options.joinToString(separator = "\n") {
                    if (it.content != null) {
                        "${it.option}. ${it.content}"
                    } else {
                        it.option
                    }
                }
            )
        )

        Log.d("openaiHandle: ", moshi.toJson(yanxi))

        val answer =
            yanxi.data.results.map {
                "--问题：" + it.question.trim() + "\n答案：" + it.answer.trim().replace("#", "\n")
            }.joinToString("\n")
//        sendNotification("题库答案：", )
        answerFlow.emit(
            "题库答案：\n$answer\nAi答案（不一定对）：${
                question.answers.joinToString(
                    separator = ","
                ) { it.trim() }
            }; ${question.answersText.trim()}"
        )
    }

    private fun getPresetContent(): String {
        return """我是一个命名实体识别 (Named Entity Recognition) 工具和解题小助手, 可以根据你给的题目信息转换成如下json格式，并且在返回的数据里面给你的问题做出解答
```json
{
  "question": "塞利格曼是( )国心理学家，主要从事习得性无助、抑郁、乐观主义、悲观主义等方面的研究。",
  "question_type": "", // 枚举：单选题、判断题、多选题、简答题，如不在这四个选项中，可自定义
  "options": [
    {
      option: "A",
      content: "美国"
    },
    {
      option: "B",
      content: "英国"
    },
    {
      option: "C",
      content: "法国"
    },
    {
      option: "D",
      content: "德国"
    },

    // 仅当 question_type 为 判断题
    {
      option: "T",
    },
    {
      option: "F",
    }

  ],
  "answers": ["A"], // 可选，当 question_type 为 单选题、判断题、多选题
  "answers_text": "", // 可选，仅当 question_type 为 简答题
  "question_details": "" // 问题详解
}
```"""
    }

    private val NOTIFICATION_CHANNEL_ID = "openai_notification_channel"
    private val NOTIFICATION_ID = 1001

    // ... (other class members and functions)

    private fun getNotification(title: String, message: String): Notification {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Fucking Ketangpai",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        return notification
    }

    override fun onDestroy() {
        super.onDestroy()
        draggableFloatingView?.remove()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

}
