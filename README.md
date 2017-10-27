[![License MIT](https://img.shields.io/badge/license-MIT-green.svg?style=flat)](https://github.com/youzan/SigmaTableViewModel/blob/master/LICENSE)&nbsp;
[![Platform](https://img.shields.io/badge/platform-Android-yellow.svg)](https://www.android.com)

<p>
<a href="https://www.youzanyun.com"><img alt="有赞logo" width="36px" src="https://img.yzcdn.cn/public_files/2017/02/09/e84aa8cbbf7852688c86218c1f3bbf17.png" alt="youzan">
</p></a>

## 有赞云App SDK(Android端)

有赞云AppSDK是为移动端应用打造的电商交易系统, 将有赞的交易服务在APP内轻松集成.

有赞AppSDK提供两种版本: **基础版**和**原生版**.

***两者的区别***:

* 基础版基于webview将有赞提供的Html5页面嵌入到App;
* 原生版着力于提供原生化的页面体验, 在后续更新升级中不断优化体验(部分页面还未原生化);
* 基础版中现商品详情页面需登录可见, 原生版中无需登录即可浏览.

***两者的相同点***:

* 都是基于视图(View)对外提供页面加载服务, 统一的调用方式基本上更换视图类即可完成SDK切换.

可根据您实际业务中对页面的要求,  选择其中一种(也可混合使用)客户端产品接入.


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

### 基础版SDK

``` groove
compile 'com.youzanyun.open.mobile:basic:5.3.2'
```

### 原生版SDK

``` groove
compile 'com.youzanyun.open.mobile:hybrid:5.3.2'
```

## 文档

[下载PDF](https://b.yzcdn.cn/youzanyun/appsdk/Youzan-SDK-Android-Doc-v5.3.0.pdf)

## 交流&反馈

* [有赞官方论坛](https://bbs.youzan.com/forum-98-1.html)
* [github issue](https://github.com/youzan/YouzanMobileSDK-Android/issues)

## License
[MIT](https://zh.wikipedia.org/wiki/MIT%E8%A8%B1%E5%8F%AF%E8%AD%89)

