package com.wilinz.fuckketangpai.data.api

import com.wilinz.fuckketangpai.data.model.YanxiQuestionBankResponse
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface YanxiApi {
    @GET("/query")
    suspend fun get(@QueryMap queryMap: Map<String,String>): YanxiQuestionBankResponse

}