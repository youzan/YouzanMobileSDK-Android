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

package com.youzanyun.sdk.sample.hybrid;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import com.youzan.sdk.YouzanHybrid;
import com.youzan.sdk.event.AbsAuthEvent;
import com.youzan.sdk.event.AbsChooserEvent;
import com.youzan.sdk.event.AbsShareEvent;
import com.youzan.sdk.event.AbsStateEvent;
import com.youzan.sdk.model.goods.GoodsShareModel;

public class YouzanActivity extends Activity implements SwipeRefreshLayout.OnRefreshListener {
    public static final String KEY_URL = "url";
    private YouzanHybrid mView;
    private SwipeRefreshLayout refreshLayout;
    private Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youzan);

        setupViews();
        setupYouzan();

        final Intent intent = getIntent();
        final String url = intent.getStringExtra(KEY_URL);

        if (mView.isFromInternal()) {
            mView.loadUrl(mView.getInternalUrl());
        } else if (!TextUtils.isEmpty(url)) {
            mView.loadUrl(url);
        }
    }


    private void setupViews() {
        mView = (YouzanHybrid) findViewById(R.id.view);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe);


        //分享按钮
        toolbar.setTitle(R.string.loading_page);
        toolbar.inflateMenu(R.menu.menu_youzan_share);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_share:
                        mView.sharePage();
                        return true;
                    default:
                        return false;
                }
            }
        });

        //刷新
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setColorSchemeColors(Color.BLUE, Color.RED);
        refreshLayout.setEnabled(false);

    }

    private void setupYouzan() {
        //认证事件, 回调表示: 需要需要新的认证信息传入
        mView.subscribe(new AbsAuthEvent() {

            @Override
            public void call(View view, boolean needLogin) {
                /**
                 * 建议实现逻辑:
                 *
                 *     判断App内的用户是否登录?
                 *       => 已登录: 请求带用户角色的认证信息(login接口);
                 *       => 未登录: needLogin为true, 唤起App内登录界面, 请求带用户角色的认证信息(login接口);
                 *       => 未登录: needLogin为false, 请求不带用户角色的认证信息(initToken接口).
                 *
                 *      服务端接入文档: https://www.youzanyun.com/docs/guide/appsdk/683
                 */
                //TODO 自行编码实现. 具体可参考开发文档中的伪代码实现
            }
        });

        //文件选择事件, 回调表示: 发起文件选择. (如果app内使用的是系统默认的文件选择器, 该事件可以直接删除)
        mView.subscribe(new AbsChooserEvent() {
            @Override
            public void call(View view, Intent intent, int requestCode) throws ActivityNotFoundException {
                startActivityForResult(intent, requestCode);
            }
        });

        //页面状态事件, 回调表示: 页面加载完成
        mView.subscribe(new AbsStateEvent() {
            @Override
            public void call(View view) {
                toolbar.setTitle(YouzanActivity.this.mView.getTitle());

                //停止刷新
                refreshLayout.setRefreshing(false);
                refreshLayout.setEnabled(true);
            }
        });

        //分享事件, 回调表示: 获取到当前页面的分享信息数据
        mView.subscribe(new AbsShareEvent() {
            @Override
            public void call(View view, GoodsShareModel data) {
                /**
                 * 在获取数据后, 可以使用其他分享SDK来提高分享体验.
                 * 这里调用系统分享来简单演示分享的过程.
                 */
                String content = data.getDesc() + data.getLink();
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, content);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, data.getTitle());
                sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        //SDK需要控制页面回退
        if (!mView.pageGoBack()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onRefresh() {
        //重新加载页面
        mView.reload();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {

            //另需处理认证等...

            mView.receiveFile(requestCode, data);
        }
    }
}
