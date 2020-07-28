提供 2 种事件总线，EventBus 和 RxBus。EventBus 做了一层包装，自动感应生命周期；RxBus 没有包装，基于 RxReley 发送事件。

## EventBus
基于 RxJava3 实现的时间总线. 自动感应 lifeOwner (Activity/fragment) 的生命周期.
```gradle
implementation "com.lwjlol:rxbus:1.0.3"
implementation 'com.uber.autodispose2:autodispose-androidx-lifecycle:2.0.0'
implementation "io.reactivex.rxjava3:rxjava:3.0.4"
implementation 'com.jakewharton.rxrelay3:rxrelay:3.0.0'
```

## usage
```
// 设置最大缓存的事件数量
EventBus.setMaxSize(3)

// 粘性事件
EventBus.instance.postSticky(EventA("123123123"))
EventBus.instance.on(EventA::class.java).observe(this) {
    Log.d(TAG, "${it.s}")
}


// 普通事件
EventBus.instance.on(String::class.java).observe(this) {
    Log.d(TAG, it)
}
EventBus.instance.post(EventA("222222222"))
```


## RxBus
其实 EventBus 的封装思想违背了 RxJava 响应式的原则，最佳做法是直接使用 RxJava 发送事件，而不是包一层。

```
// 粘性事件
RxBus.postSticky("RxBus postSticky 2")
RxBus.eventSticky(String::class.java).subscribe {
    Log.d(TAG, "${it}")
}


// 普通事件
RxBus.post("RxBus  2")
RxBus.event(String::class.java).subscribe {
    Log.d(TAG, "${it}")
}
```
