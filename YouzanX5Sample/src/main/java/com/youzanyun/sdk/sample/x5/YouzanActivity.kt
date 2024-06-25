/*
 * Copyright (C) 2017 youzanyun.com, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.youzanyun.sdk.sample.x5

import android.app.AppOpsManager
import android.app.AsyncNotedAppOp
import android.app.SyncNotedAppOp
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log

class YouzanActivity : AppCompatActivity() {
    private var mFragment: YouzanFragment? = null
    private val MY_APP_TAG = "MY_APP_TAG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_placeholder)



        mFragment = YouzanFragment()
        mFragment!!.arguments = intent.extras
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.placeholder, mFragment!!)
            .commitAllowingStateLoss()
    }


    override fun onResume() {
        super.onResume()
    }

    override fun onBackPressed() {
        if (mFragment == null || !mFragment!!.onBackPressed()) {
            super.onBackPressed()
        }
    }

    companion object {
        const val KEY_URL = "url"
    }
}