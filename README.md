[![License MIT](https://img.shields.io/badge/license-MIT-green.svg?style=flat)](https://github.com/youzan/SigmaTableViewModel/blob/master/LICENSE)&nbsp;
[![Platform](https://img.shields.io/badge/platform-Android-yellow.svg)](https://www.android.com)

[集成腾讯X5内核的开店SDK][![Release](https://img.shields.io/badge/release-6.4.9-red.svg)](https://bintray.com/youzanyun/maven/)
（注意：如果从原生WebView的SDK版本升级到X5内核的SDK版本时，请务必查看[相关文档](https://github.com/youzan/YouzanMobileSDK-Android/wiki/%E5%9F%BA%E4%BA%8E%E5%8E%9F%E7%94%9FWebView%E7%9A%84%E5%BC%80%E5%BA%97SDK%E5%88%87%E6%8D%A2%E5%88%B0X5%E7%89%88%E6%9C%AC%E6%B3%A8%E6%84%8F%E4%BA%8B%E9%A1%B9)）


[基于原生WebView的开店SDK][![Release](https://img.shields.io/badge/release-6.4.9-red.svg)](https://bintray.com/youzanyun/maven/)不建议继续使用

<p>
<a href="https://www.youzanyun.com"><img alt="有赞logo" width="36px" src="https://img.yzcdn.cn/public_files/2017/02/09/e84aa8cbbf7852688c86218c1f3bbf17.png" alt="youzan">
</p></a>

## 有赞云App SDK(Android端)

为了满足移动应用搭建商城的需求，有赞云将有赞积累多年的电商交易系统开放，移动开发者**通过一个 SDK 便可以在 App 内集成有赞提供的整个交易服务**，除了享受完善的商城功能、丰富的营销玩法，更有有赞强劲的技术及服务作保障，实现低成本、高效率、强融合的移动电商方案，快速获得 App 流量的商业化变现。该SDK基于 WebView 将有赞提供的 HTML5 页面嵌入到 App 中，基于此提供帐号打通、资产合并、客服 IM、多渠道支付、营销能力开放等 App 应用特色功能，更拥有媲美原生页面的性能。

### X5内核版本开店SDK（推荐使用）
依托腾讯浏览服务的开店SDK，加载速度更快，兼容性与安全性更好，视频性能更出色。安装包大约增加400k

### 系统原生WebView（不建议继续使用）
基于系统原生WebView提供服务，在兼容性与性能上比X5内核版本较差，不建议继续使用。

**最新的H5 版 SDK已经支持商品详情页免登录，想要在H5 版实现商品详情页免登的开发者可以联系 有赞阿星：youzanmin （微信）添加白名单，提供：应用的clientid，店铺名称。**

对接过程中有产品需求、建议，或是想了解 AppSDK 更多玩法，请随时联系有赞阿星：youzanmin （微信），售前技术支持请联系有赞朱昌辉：youzan_dazhu （微信）或者 18106503919 （电话）

## 引入

在项目根目录的build.gradle中声明maven仓库, 如下所示:

``` groove
allprojects {
    repositories {
        jcenter()
        maven {url 'https://dl.bintray.com/youzanyun/maven/'}
    }
}
```

在子项目build.gradle的dependencies中根据需求引入依赖:

基于腾讯x5内核的开店SDK：
``` groovy
compile 'com.youzanyun.open.mobile:x5sdk:6.4.9'
```
基于Android原生WebView的开店SDK（不建议使用）：
``` groovy
compile 'com.youzanyun.open.mobile:basic:6.4.9'
```

## 文档
详细的接入文档见
[开发文档](https://github.com/youzan/YouzanMobileSDK-Android/wiki)

## 交流&反馈

* [有赞官方论坛](https://bbs.youzan.com/forum-98-1.html)
* [github issue](https://github.com/youzan/YouzanMobileSDK-Android/issues)

## License
[MIT](https://zh.wikipedia.org/wiki/MIT%E8%A8%B1%E5%8F%AF%E8%AD%89)

