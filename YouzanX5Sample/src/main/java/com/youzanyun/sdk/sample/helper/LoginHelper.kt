package com.youzanyun.sdk.sample.helper

import android.content.Context
import android.content.SharedPreferences

/**
 * auther: liusaideng
 * created on :  2023/7/17 7:39 PM
 * desc:
 */
object LoginHelper {
    private var sp: SharedPreferences? = null


    fun init(context: Context) {
        sp = context.getSharedPreferences("x5", Context.MODE_PRIVATE)
    }

    fun isLogin(): Boolean {
        return sp?.getBoolean("is_login", false) == true
    }

    fun setLogin(isLogin: Boolean) {
        sp?.edit()?.putBoolean("is_login", isLogin)?.apply()
    }
}