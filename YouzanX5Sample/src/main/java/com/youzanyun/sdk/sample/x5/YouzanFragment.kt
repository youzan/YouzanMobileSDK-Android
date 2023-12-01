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

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.widget.Toast
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import com.youzan.androidsdk.event.*
import com.youzan.androidsdk.model.goods.GoodsShareModel
import com.youzan.androidsdk.model.refresh.RefreshChangeModel
import com.youzan.androidsdk.model.trade.TradePayFinishedModel
import com.youzan.androidsdkx5.YouzanBrowser
import com.youzan.androidsdkx5.compat.CompatWebChromeClient
import com.youzan.androidsdkx5.compat.VideoCallback
import com.youzan.androidsdkx5.compat.WebChromeClientConfig
import com.youzanyun.sdk.sample.helper.YouzanHelper
import org.json.JSONException
import org.json.JSONObject

/**
 * 这里使用[WebViewFragment]对[android.webkit.WebView]生命周期有更好的管控.
 */
class YouzanFragment : WebViewFragment(), OnRefreshListener {
    private lateinit var mView: YouzanBrowser
    private val mRefreshLayout: SwipeRefreshLayout? = null
    private var mToolbar: Toolbar? = null

    companion object {
        private const val CODE_REQUEST_LOGIN = 0x1000

        fun newInstance(url: String) : Fragment{
            val fg = YouzanFragment()
            fg.arguments = Bundle().apply {
                putString(YouzanActivity.KEY_URL, url)
            }
            return fg
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.back).setOnClickListener {
            Log.d("lsd", "click")
        }
        setupViews(view)
        setupYouzan()
        val url : String? = arguments!!.getString(YouzanActivity.KEY_URL)
        if (url != null) {
            mView.loadUrl(url)
        }


        //加载H5时，开启默认loading
        //设置自定义loading图片
//        mView.setLoadingImage(R.mipmap.ic_launcher);
    }

    private fun setupViews(contentView: View) {
        //WebView
        mView = webView
        if (mView.getX5WebViewExtension() != null) {
            val data = Bundle()
            data.putBoolean("standardFullScreen", true) // true表示标准全屏，false表示X5全屏；不设置默认false，
            data.putBoolean("supportLiteWnd", true) // false：关闭小窗；true：开启小窗；不设置默认true，
            data.putInt("DefaultVideoScreen", 2) // 1：以页面内开始播放，2：以全屏开始播放；不设置默认：1
            mView.getX5WebViewExtension().invokeMiscMethod("setVideoParams", data)
        }
        mToolbar = contentView.findViewById<View>(R.id.toolbar) as Toolbar
        //        mRefreshLayout = (SwipeRefreshLayout) contentView.findViewById(R.id.swipe);

        //分享按钮
        mToolbar!!.setTitle(R.string.loading_page)
        mToolbar!!.inflateMenu(R.menu.menu_youzan_share)
        mToolbar!!.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_share -> {
                    mView.sharePage()
                    true
                }
                R.id.action_refresh -> {
                    mView.reload()
                    true
                }
                else -> false
            }
        }

        //刷新
//        mRefreshLayout.setOnRefreshListener(this);
//        mRefreshLayout.setColorSchemeColors(Color.BLUE, Color.RED);
//        mRefreshLayout.setEnabled(false);
        mView.setWebChromeClient(object : WebChromeClient() {
            override fun onShowCustomView(view: View, customViewCallback: IX5WebChromeClient.CustomViewCallback) {
                super.onShowCustomView(view, customViewCallback)
                customViewCallback.onCustomViewHidden() // 避免视频未播放时，点击全屏白屏的问题
            }


        })

        mView.setWebChromeClient(CompatWebChromeClient(
            WebChromeClientConfig(
                true, object : VideoCallback {
                    override fun onVideoCallback(b: Boolean) {
                        Toast.makeText(activity, "" + b, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        ))

        mView.setWebViewClient(object : WebViewClient() {
            override fun onPageFinished(p0: WebView?, p1: String?) {
                super.onPageFinished(p0, p1)
                Log.d("lsd", "onPageFinished")
            }

            override fun onPageStarted(p0: WebView?, p1: String?, p2: Bitmap?) {
                super.onPageStarted(p0, p1, p2)
                Log.d("lsd", "onPageStarted")

            }


            override fun shouldOverrideUrlLoading(p0: WebView?, p1: WebResourceRequest?): Boolean {
                if (p1?.url?.scheme == "http") {
                    Log.d("lsd", p1.url?.toString() + "")
                    Toast.makeText(activity!!, "出现http", Toast.LENGTH_LONG).show()
                    return true
                }

                return super.shouldOverrideUrlLoading(p0, p1)
            }

            override fun shouldOverrideUrlLoading(p0: WebView?, p1: String?): Boolean {
                if (p1?.startsWith("http://") ==true ) {
                    Log.d("lsd", p1 +  "")
                    Toast.makeText(activity!!, "出现http", Toast.LENGTH_LONG).show()
                    return true
                }

                return super.shouldOverrideUrlLoading(p0, p1)
            }
        })
    }

    private fun setupYouzan() {

        mView!!.subscribe(object : AbsCheckAuthMobileEvent() {})
        //认证事件, 回调表示: 需要需要新的认证信息传入
        mView!!.subscribe(object : AbsAuthEvent() {
            override fun call(context: Context, needLogin: Boolean) {
                /**
                 * 建议实现逻辑:
                 *
                 * 判断App内的用户是否登录?
                 * => 已登录: 请求带用户角色的认证信息(login接口);
                 * => 未登录: needLogin为true, 唤起App内登录界面, 请求带用户角色的认证信息(login接口);
                 * => 未登录: needLogin为false, 请求不带用户角色的认证信息(initToken接口).
                 *
                 * 服务端接入文档: https://www.youzanyun.com/docs/guide/appsdk/683
                 */
                //TODO 自行编码实现. 具体可参考开发文档中的伪代码实现
                //TODO 手机号自己填入
                YouzanHelper.loginYouzan(activity!!, {
                    mView.reload()
                })

            }
        })
        mView!!.subscribe(object : AbsCheckAuthMobileEvent() {})
        //文件选择事件, 回调表示: 发起文件选择. (如果app内使用的是系统默认的文件选择器, 该事件可以直接删除)
        mView!!.subscribe(object : AbsChooserEvent() {
            @kotlin.jvm.Throws(ActivityNotFoundException::class)
            override fun call(context: Context, intent: Intent, requestCode: Int) {
                startActivityForResult(intent, requestCode)
            }
        })
        mView!!.subscribe(object : AbsChangePullRefreshEvent() {
            override fun call(refreshChangeModel: RefreshChangeModel?) {
                if (refreshChangeModel != null && refreshChangeModel.enable != null) {
                    //新建收货地址页下滑与页面下拉刷新冲突时，禁止该页面下拉刷新
//                    mRefreshLayout.setEnabled(refreshChangeModel.getEnable());
                }
            }
        })

        //页面状态事件, 回调表示: 页面加载完成
        mView!!.subscribe(object : AbsStateEvent() {
            override fun call(context: Context) {
                mToolbar!!.title = mView!!.title
                //停止刷新
//                mRefreshLayout.setRefreshing(false);
//                mRefreshLayout.setEnabled(true);
            }
        })
        mView!!.subscribe(object : AbsCustomEvent() {
            override fun callAction(context: Context, action: String, data: String) {
                when (action) {
                    "openHome" ->                         //此处仅举例，具体实现根据对应需求做调整
                        try {
                            val jsonObject = JSONObject(data)
                            val paramObj = jsonObject.optJSONObject("params")
                            val result = paramObj.optString("test")
                            Toast.makeText(activity, "test:$result", Toast.LENGTH_LONG).show()
                        } catch (e: JSONException) {
                            throw RuntimeException(e)
                        }
                }
            }
        })
        //分享事件, 回调表示: 获取到当前页面的分享信息数据
        mView!!.subscribe(object : AbsShareEvent() {
            override fun call(context: Context, data: GoodsShareModel) {
                /**
                 * 在获取数据后, 可以使用其他分享SDK来提高分享体验.
                 * 这里调用系统分享来简单演示分享的过程.
                 */
                val content = data.desc + data.link
                val sendIntent = Intent()
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.putExtra(Intent.EXTRA_TEXT, content)
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, data.title)
                sendIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                sendIntent.type = "text/plain"
                startActivity(sendIntent)
            }
        })
        mView!!.subscribe(object : AbsPaymentFinishedEvent() {
            override fun call(context: Context, tradePayFinishedModel: TradePayFinishedModel) {}
        })
    }

    override fun getWebViewId(): Int {
        //YouzanBrowser在布局文件中的id
        return R.id.view
    }

    override fun getLayoutId(): Int {
        //布局文件
        return R.layout.fragment_youzan
    }

    override fun onBackPressed(): Boolean {
        //页面回退
        return webView.pageGoBack()
    }

    override fun onRefresh() {
        //重新加载页面
        mView!!.reload()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (CODE_REQUEST_LOGIN == requestCode) { // 如果是登录事件返回
            if (resultCode == Activity.RESULT_OK) {
                // 登录成功设置token
            } else {
                // 登录失败
                mView!!.syncNot()
            }
        } else {
            // 文件选择事件处理。
            mView!!.receiveFile(requestCode, data)
        }
    }
}