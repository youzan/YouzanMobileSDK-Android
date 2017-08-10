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

package com.youzanyun.sdk.sample.basic;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.youzan.sdk.YouzanSDK;

public class MainActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.button_open).setOnClickListener(this);
        findViewById(R.id.button_clear).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_open:
                //店铺链接, 可以从有赞后台`店铺=>店铺概况=>访问店铺`复制到相应的链接
                gotoActivity("https://h5.youzan.com/v2/showcase/homepage?alias=kewr19e1");
                break;
            case R.id.button_clear:
                YouzanSDK.userLogout(this);
                break;
            default:
                break;
        }
    }

    private void  gotoActivity(String url){
        Intent intent = new Intent(this, YouzanActivity.class);
        intent.putExtra(YouzanActivity.KEY_URL, url);
        startActivity(intent);
    }
}