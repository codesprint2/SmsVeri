package com.example.smsveri.comm

import com.example.smsveri.model.SmsCallback
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiClient {
    @POST("/api/sms/recipient")
    fun sendMeCode(@Body smsCallback: SmsCallback) : Call<Void>
}