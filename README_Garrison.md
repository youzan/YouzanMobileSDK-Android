
## 功能介绍：
https://developers.weixin.qq.com/community/develop/article/doc/000aa458920890f83bad43f6c57013

## hook的隐私api:
| 隐私类 | 方法  | 是否支持缓存能力|
| ------ |------ |------ |
| AccountManager | getAccounts() | 是|
| AccountManager | getAccountsByType(type) |否|
| BluetoothLeScanner| startScan(callback) |是|
| TelephonyManager | getDeviceId() |是|
| TelephonyManager | getImei() | 是|
| TelephonyManager | getImei(slotIndex) | 是|
| TelephonyManager | getCellLocation() | 否|
| TelephonyManager | getAllCellInfo() | 是|
| TelephonyManager | getSubscriberId() |是|
| TelephonyManager | getSimSerialNumber() | 是|
| TelephonyManager | getLine1Number() | 是|
| TelephonyManager | getNetworkOperator() | 是|
| TelephonyManager | getSimCountryIso() | 是|
| LocationManager | getCellLocation() | 否|
| LocationManager | requestLocationUpdates(provider, minTime, minDistance, listener, looper) | 否|
| LocationManager| requestLocationUpdates(minTime, minDistance, criteria, listener, looper) | 否|
| LocationManager | requestLocationUpdates(provider, minTime, minDistance, intent) | 否|
| LocationManager | requestLocationUpdates(minTime, minDistance, criteria, intent) | 否|
| PackageManager | getInstalledApplications(flags) | 是|
| PackageManager | getInstalledPackages(flags) | 是|
| WifiInfo | getMacAddress() | 是|
| WifiInfo | getSSID() | 是|
| WifiInfo | getBSSID() | 是|
| WifiInfo | getScanResults() | 是|
| WifiInfo | getConnectionInfo() | 是|
| NetworkInterface | getHardwareAddress() | 是|
| NetworkInterface | getInetAddresses() | 否|
| ActivityManager | getRunningAppProcesses() | 否 |
| ActivityManager | getRunningTasks(maxNum) | 否 |
| SensorManager | registerListener(listener, sensor, samplingPeriodUs, handler) | 否 |
| SensorManager | getSensorList(type) | 否 |
| Settings.Secure | getString(resolver, name) | 是 |



## 版本记录
### 主sdk版本更新历史
| 版本 | 时间 | 升级内容  |
| ------ | ------ | ------ |
| 0.3.0 | 2022-12-15 | 1.增加getInetAddresses 设备ip hook方法（没有缓存能力） |
| 0.2.0 | 2022-10-15 | 1.通过api hook统一处理隐私api问题，并增加缓存能力，避免调用频率过高 |

### 插件更新历史
| 版本 | 时间 | 升级内容  |
| ------ | ------ | ------ |
| 0.2.0 | 2022-10-15 | 1. 增加隐私api编译时hook能力 |

## 接入姿势
### 依赖配置
```
// 主工程build.gradle文件依赖garrison sdk
implementation "com.youzanyun.open.mobile:garrison:${version}"

// 主工程build.gradle文件依赖plugin文件
apply plugin: 'com.youzan.garrison.plugin'

// 根目录build.gradle文件，增加classpath
classpath "com.youzanyun.open.mobile:garrison_plugin:${version}"
```

### maven配置
```
maven { url 'http://maven.youzanyun.com/repository/maven-releases' }
```

### 初始化逻辑
*建议在Application onCreate方法首先调用*
```java
//  isDebug = true环境下，调用隐私api会在logcat中输出日志
GarrisonEngine.Builder().isDebug(isDebug).setContext(this).build()
```

### 是否开启隐私api缓存能力
*设置缓存能力开启状态时，隐私api最多全局调用3次，默认开始状态*
```java
GarrisonEngine.isCacheApi = true | false
```

### 日志查看姿势
* 查看隐私api是否被调用
```
adb logcat -s BaseDelegate
```

* 查看隐私api是否使用了缓存
```
adb logcat -s RestrictAPICache
```
