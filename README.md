## ESurfingClient-CVersion Android 移植总结

由于我对git和github的不熟悉，加上前面Push和commit时不仔细，导致一堆问题，因此我用最蠢的方法删库重建，所以之前star过的需要重新star一下。还有由于之前打的Tag就不一定匹配对应的版本，所以现在的Tag也不一定对应当时版本的代码。
本项目已成功将原始 C 语言编写的天翼校园网认证客户端(来自[BadGhost520](https://github.com/BadGhost520))移植为 Android 原生应用(APK)。通过 JNI 桥接和 Android 前台服务，实现了无须 shell 或 Root 权限的校园网拨号认证。

[转到原作者的项目](https://github.com/BadGhost520/ESurfingClient-CVersion) | [回到原先我修改的分支](https://github.com/anshenglv/ESurfingClient-CVersion-Android-arm-v8a)

**应用运行示例：**

<img alt="Please refresh" height="569" src="img/Home.jpg" width="256"/> <img alt="Please refresh" height="569" src="img/Log.jpg" width="256"/>
<img alt="Please refresh" src="img/Full.jpg" width="516"/>

### 1. 核心架构变更
- 构建系统：从纯 CMake 迁移至 Android Gradle + CMake (NDK) 体系。
- 输出格式：由可执行二进制文件转变为共享库 (.so)，通过 JNI 被 Android 应用调用。
- 运行模式：采用 Android 前台服务 (Foreground Service) 包装 C 逻辑，并提供持续的通知栏显示。
### 2. 原生 C 代码处理
- 尽量少地改动原始C代码，非必要问题不处理，方便日后同步更新。
- 添加了一堆#ifdef __ANDROID__，用来避免执行没有的功能和没有的路径。
- 不在Android上使用Web前端功能。
- 日志系统：增加了 Android 日志 (__android_log_print) 支持，方便在 Logcat 中调试。
### 3. JNI 桥接层设计
- shut()退出逻辑重构，避免整个应用被阻塞和闪退。
- 状态同步：通过原子标志位(g_thread_keep_alive)
同步Java层与C层的运行状态，同步防止用户点击了停止后又立即启动。
### 4. Android UI/UX 实现
- 实时日志查看器：
  - 只有在服务运行时每0.5秒自动从run.log读取日志并同步到应用层。
  - “清除日志”清除的是历史日志并清空run.log，历史日志(已被重命名为时间的)可供root用户自行查找。
  - 双指缩放：支持通过捏合手势实时调整日志字体大小，但是最好横向捏合，新版compose纵向容易误触滑动。
- 多语言支持：建立了完善的 strings.xml 资源体系，支持 中文 / 英文 根据系统语言自动切换。
- 沉浸式设计：
  - 适配 Edge-to-Edge 全屏显示。
  - 沉浸式状态栏，自动切换深浅色。
  - 安卓12+可以根据手机背景动态取色，否则显示默认紫色主题色。
### 5. 体积与性能优化
- 可能是用了新版的库编译，编译的静态库太大，不然apk可以到4MB左右。
- 架构精简：只编译了 arm64-v8a 指令集。
### 6. 安全与权限
- 适配 Android 13/14+：
  - 正确声明了 FOREGROUND_SERVICE_SPECIAL_USE。
  - 实现了运行时通知权限申请。
