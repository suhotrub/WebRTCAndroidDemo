package com.suhotrub.webrtcandroiddemo.network

import com.google.gson.Gson
import java.lang.Exception

object MessageConverter {

    inline fun <reified T> fromJs(message: String): T? {
        return try {
            Gson().fromJson(message, T::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun toJs(data: Any) = Gson().toJson(data)!!

}