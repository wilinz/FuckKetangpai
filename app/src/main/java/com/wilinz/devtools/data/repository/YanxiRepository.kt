package com.wilinz.devtools.data.repository

import android.util.Log
import com.wilinz.devtools.data.Network
import com.wilinz.devtools.data.Question
import com.wilinz.devtools.data.YanxiQuestionBankRequest
import com.wilinz.devtools.data.YanxiQuestionBankResponse
import toMap

object YanxiRepository {
    suspend fun get(request: YanxiQuestionBankRequest): YanxiQuestionBankResponse {
        Log.d("YanxiRepository", request.toString())
        return Network.yanxiApi.get(request.toMap())
    }

}