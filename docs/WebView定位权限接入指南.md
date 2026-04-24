# 有赞 X5 WebView H5 定位权限接入指南

## 1. 背景说明
当嵌入 WebView 的 H5 页面（如有赞同城配送、门店选择等业务）通过前端 JS 调用 `navigator.geolocation` 获取用户地理位置时，不仅需要 H5 自身的授权，还需要 Android 客户端提供原生的**定位权限（Runtime Permissions）**支持。

本指南说明了如何在有赞 SDK 的 `YouzanFragment` 中正确拦截网页定位请求，并与 Android 动态权限申请机制结合。

## 2. 接入步骤

### 2.1 清单文件声明权限
首先确保在 `AndroidManifest.xml` 中已经声明了网络和定位相关的权限：
```xml
<!-- 允许应用程序联网 -->
<uses-permission android:name="android.permission.INTERNET" />
<!-- 获取地理信息的权限 -->
<uses-permission android:name="android.permission.ACCESS_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### 2.2 开启 WebView 定位能力
在初始化 WebView 时，需要确保 WebSettings 的定位功能已开启。
**代码位置**：[YouzanFragment.kt](file:///Users/daye/youzan/YouzanSDKAndroid/github1/YouzanX5Sample/src/main/java/com/youzanyun/sdk/sample/x5/YouzanFragment.kt) 的 `onViewCreated` 或配置视图的方法中：

```kotlin
val settings = webView.settings
settings.setGeolocationEnabled(true) // 开启 WebView 定位功能
```

### 2.3 拦截 H5 的定位授权请求
当网页发起定位请求时，会触发 `WebChromeClient` 的 `onGeolocationPermissionsShowPrompt` 回调。我们需要在这里检查 Android 系统的定位权限是否已授予。

在 `YouzanFragment` 中增加临时变量保存网页回调：
```kotlin
private var mGeolocationCallback: GeolocationPermissionsCallback? = null
private var mGeolocationOrigin: String? = null
```

重写 `CompatWebChromeClient` 的相关方法：
```kotlin
mView.setWebChromeClient(object: CompatWebChromeClient(config) {
    // ... 其他回调

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissionsCallback?
    ) {
        val activity = activity ?: return
        
        // 检查 Android 运行时权限
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            
            // 尚未授权，保存网页的 callback 和 origin
            mGeolocationCallback = callback
            mGeolocationOrigin = origin
            
            // 发起 Android 运行时权限请求 (REQUEST_CODE 设为 1001)
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION, 
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), 
                1001
            )
        } else {
            // 已经有权限，直接通知网页允许获取定位
            // invoke 参数: (origin, allow, retain)
            callback?.invoke(origin, true, false)
        }
    }
})
```

### 2.4 将系统授权结果回传给网页
在 Fragment (或 Activity) 的 `onRequestPermissionsResult` 方法中，接收用户的授权结果，并通过之前保存的 `GeolocationPermissionsCallback` 回传给 X5 WebView，网页即可开始获取定位。

```kotlin
override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    
    if (requestCode == 1001) {
        // 判断用户是否授予了定位权限
        val isGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        
        // 将结果回传给 WebView
        mGeolocationCallback?.invoke(mGeolocationOrigin, isGranted, false)
        
        // 清理内存，防止泄漏
        mGeolocationCallback = null
        mGeolocationOrigin = null
    }
}
```

## 3. 注意事项
1. **权限请求来源**：如果您在 `Fragment` 中调用 `requestPermissions`，请重写 `Fragment` 的 `onRequestPermissionsResult`；如果通过 `Activity` 申请，请重写 `Activity` 的回调，确保能正确拦截到结果。
2. **隐私合规**：建议在调用 `requestPermissions` 之前，根据工信部合规要求，先向用户弹窗说明为什么需要使用定位权限（例如：“为了向您展示附近的门店，我们需要获取您的位置信息”）。
3. **Android 12 (API 31) 适配**：如果是高版本系统，申请 `ACCESS_FINE_LOCATION` 时通常需要同时申请 `ACCESS_COARSE_LOCATION`。
