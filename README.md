[![License MIT](https://img.shields.io/badge/license-MIT-green.svg?style=flat)](https://github.com/youzan/SigmaTableViewModel/blob/master/LICENSE)&nbsp;
[![Platform](https://img.shields.io/badge/platform-Android-yellow.svg)](https://www.android.com)
[![Release](https://img.shields.io/badge/release-6.0.1-red.svg)](https://bintray.com/youzanyun/maven/)

<p>
<a href="https://www.youzanyun.com"><img alt="有赞logo" width="36px" src="https://img.yzcdn.cn/public_files/2017/02/09/e84aa8cbbf7852688c86218c1f3bbf17.png" alt="youzan">
</p></a>

## 有赞云App SDK(Android端)

为了满足移动应用搭建商城的需求，有赞云将有赞积累多年的电商交易系统开放，移动开发者**通过一个 SDK 便可以在 App 内集成有赞提供的整个交易服务**，除了享受完善的商城功能、丰富的营销玩法，更有有赞强劲的技术及服务作保障，实现低成本、高效率、强融合的移动电商方案，快速获得 App 流量的商业化变现。

有赞 AppSDK 提供两种版本方案: H5 版 SDK 和混合版 SDK，客户端根据需求选择两者之一接入即可。两个版本的特点及区别在于:

- **H5 版 SDK**：基于 WebView 将有赞提供的 HTML5 页面嵌入到 App 中，基于此提供帐号打通、资产合并、客服 IM、多渠道支付、营销能力开放等 App 应用特色功能，更拥有媲美原生页面的性能。该方案由于接入极速、功能完善、性能稳定，现已被大多数电商 App 开发者采用。建议优先选用。

  **最新的H5 版 SDK已经支持商品详情页免登录，想要在H5 版实现商品详情页免登的开发者可以联系 有赞雨果（微信：13738185176）添加白名单，提供：应用的clientid，店铺名称。**

- **混合版 SDK （公测中）**：部分 App 开发者对于商城页面有个性化的需求，基于此，我们推出混合版 SDK，在 H5 版的基础上，提供部分原生页面，包括商品详情页、订单列表页、购物车页、支付结果页等，这些页面提供了更加灵活的定制能力。目前该版本可以满足基本的电商业务场景, 但对于客服功能、数据分析功能及部分复杂的营销功能，会在后续的更新升级中不断优化体验。

对接过程中有任何问题、需求、建议，或是想了解 AppSDK 更多玩法，请随时联系有赞雨果，13738185176 （微信&电话）

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

``` groove
compile 'com.youzanyun.open.mobile:basic:6.3.0'
```

## 文档

[下载PDF](https://b.yzcdn.cn/youzanyun/appsdk/Youzan-SDK-Android-Doc-v6.3.0.pdf)

## 交流&反馈

* [有赞官方论坛](https://bbs.youzan.com/forum-98-1.html)
* [github issue](https://github.com/youzan/YouzanMobileSDK-Android/issues)

## License
[MIT](https://zh.wikipedia.org/wiki/MIT%E8%A8%B1%E5%8F%AF%E8%AD%89)

