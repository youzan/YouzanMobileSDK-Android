package com.youzanyun.sdk.sample.x5

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.TextureView
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.tencent.smtt.sdk.QbSdk
import com.youzan.androidsdk.YouzanSDK
import com.youzan.androidsdkx5.YouzanPreloader
import com.youzanyun.sdk.sample.config.KaeConfig
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        findViewById<View>(R.id.go_with_login).setOnClickListener { goWithLogin() }
        findViewById<View>(R.id.go_without_login).setOnClickListener { go() }
        findViewById<EditText>(R.id.url).apply {
              setText("https://cashier.youzan.com/pay/wsctrade_pay?kdt_id=96501531&orderNo=E20250901150406079500079&banner_id=f.96009711_uc.119216149_g.1292024367~pending_payment_popup~0~FT56o2ri")
        }
        findViewById<TextView>(R.id.go).setOnClickListener {
            val url: String = findViewById<EditText>(R.id.url).text.toString()
            if (url.startsWith("http")) {
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                intent.putExtra("url", url)
                startActivity(intent)
            }
        }


        findViewById<View>(R.id.logout).setOnClickListener {
//          YouzanSDK.userLogout(this@SplashActivity)
            Toast.makeText(this@SplashActivity, "${QbSdk.isX5Core()}， ${QbSdk.canLoadX5(this@SplashActivity)}", Toast.LENGTH_SHORT).show()
        }

    }

    fun goWithLogin() {
        val clz = LoginActivity::class.java
        val intent = Intent(this@SplashActivity, clz)
        startActivity(intent)
    }

    fun go() {
        val clz = MainActivity::class.java
        val intent = Intent(this@SplashActivity, clz)
        startActivity(intent)
    }


    fun getAppSignatureSHA1(context: Context, type : String = "SHA1" ): String? {
        return try {
            val packageName = context.packageName
            val packageManager = context.packageManager

            // 获取签名信息（兼容 API 28+ 和 低版本）
            val packageInfo: PackageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                // Android 9.0 (API 28) 及以上，使用 GET_SIGNING_CERTIFICATES
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            // 提取签名数组
            val signatures: Array<Signature> = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {

                // API 28+，使用 signingInfo 获取签名
                packageInfo.signingInfo.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isEmpty()) {
                return null
            }

            // 通常只取第一个签名（绝大多数应用只有一个签名）
            val signature = signatures[0]
            val md = MessageDigest.getInstance(type)
            md.update(signature.toByteArray())
            val digest = md.digest()

            // 转换为十六进制字符串，格式如：12:34:56:78:90:AB:...
            val hexString = StringBuilder()
            for (b in digest) {
                val hex = Integer.toHexString(0xFF and b.toInt())
                if (hex.length == 1) hexString.append('0')
                hexString.append(hex)
                hexString.append(':') // 可选：加上冒号分隔，更易读
            }
            // 移除最后一个多余的冒号（可选）
            if (hexString.isNotEmpty() && hexString.last() == ':') {
                hexString.deleteCharAt(hexString.length - 1)
            }

            hexString.toString()
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        } catch (e: NoSuchAlgorithmException) {

            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}