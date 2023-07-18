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

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.view.View
import com.ashokvarma.bottomnavigation.BottomNavigationBar
import com.ashokvarma.bottomnavigation.BottomNavigationBar.MODE_FIXED
import com.ashokvarma.bottomnavigation.BottomNavigationBar.OnTabSelectedListener
import com.ashokvarma.bottomnavigation.BottomNavigationItem
import com.youzanyun.sdk.sample.config.KaeConfig

class MainActivity : FragmentActivity(), View.OnClickListener {
    private lateinit var mBottomNavigator: BottomNavigationBar
    private lateinit var mViewPager: ViewPager
    private val fgLists = mutableListOf<Fragment>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mBottomNavigator = findViewById(R.id.bottom_navigator)
        mBottomNavigator.addItem(BottomNavigationItem(R.drawable.ic_launcher, "主页"))
            .addItem(BottomNavigationItem(R.drawable.ic_launcher, "购物车"))
            .addItem(BottomNavigationItem(R.drawable.ic_launcher, "个人中心"))
            .addItem(BottomNavigationItem(R.drawable.ic_launcher, "退出入口"))
            .setMode(MODE_FIXED)
            .initialise()
        mBottomNavigator.setBackgroundResource(R.color.x5_grey)

        mBottomNavigator.setTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(position: Int) {
                mViewPager.setCurrentItem(position, true)
            }

            override fun onTabUnselected(position: Int) {

            }

            override fun onTabReselected(position: Int) {

            }
        })


        mViewPager = findViewById(R.id.vp)
        mViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
            }

            override fun onPageSelected(p0: Int) {
                mBottomNavigator.selectTab(p0, false)
            }

            override fun onPageScrollStateChanged(p0: Int) {
            }

        })
        mViewPager.offscreenPageLimit = 3
        val fg0 = YouzanFragment.newInstance(KaeConfig.S_URL_MAIN)
        val fg1 = YouzanFragment.newInstance(KaeConfig.S_URL_MART)
        val fg2 = YouzanFragment.newInstance(KaeConfig.S_URL_person)
        val fg3 = LogoutFragment()

        fgLists.add(fg0)
        fgLists.add(fg1)
        fgLists.add(fg2)
        fgLists.add(fg3)
        mViewPager.adapter = object : FragmentPagerAdapter(supportFragmentManager) {
            override fun getCount(): Int {
                return 4
            }

            override fun getItem(p0: Int): Fragment {
                return when (p0) {
                    0 -> fg0
                    1 -> fg1
                    2 -> fg2
                    3 -> fg3
                    else -> fg3
                }
            }


        }
    }

//    override fun onClick(v: View) {
//        when (v.id) {
//            R.id.button_open -> {
//                //店铺链接, 可以从有赞后台`店铺=>店铺概况=>访问店铺`复制到相应的链接，这里是一个测试链接
//                var url: String
//                //
////                        "https://shop118687317.m.youzan.com/wscshop/showcase/homepage?kdt_id=118495149";
////                "https://h5.youzan.com/v2/showcase/homepage?alias=lUWblj8NNI";
//                gotoActivity(url)
//            }
//            R.id.button_clear -> YouzanSDK.userLogout(this)
//            else -> {}
//        }
//    }

    private fun gotoActivity(url: String) {
        val intent = Intent(this, YouzanActivity::class.java)
        intent.putExtra(YouzanActivity.KEY_URL, url)
        startActivity(intent)
    }

    override fun onClick(v: View?) {

    }

    override fun onBackPressed() {
        if ((fgLists.get(index = mViewPager.currentItem) as? YouzanFragment)?.onBackPressed() == true) {
            return
        }

        super.onBackPressed()
    }
}


