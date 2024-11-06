package com.wilinz.fuckketangpai.data.repository

import android.util.Log
import com.wilinz.fuckketangpai.data.Network
import com.wilinz.fuckketangpai.data.model.YanxiQuestionBankRequest
import com.wilinz.fuckketangpai.data.model.YanxiQuestionBankResponse
import toMap

object YanxiRepository {
    suspend fun get(request: YanxiQuestionBankRequest): YanxiQuestionBankResponse {
        Log.d("YanxiRepository", request.toString())
        return Network.yanxiApi.get(request.toMap())
    }

}