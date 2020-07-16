package com.lwjlol.eventbus

import android.util.Log
import android.util.LruCache
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import autodispose2.AutoDispose
import autodispose2.androidx.lifecycle.AndroidLifecycleScopeProvider
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.processors.BehaviorProcessor
import io.reactivex.rxjava3.processors.FlowableProcessor
import io.reactivex.rxjava3.processors.PublishProcessor

/**
 * @author luwenjie on 2019-07-05 18:11:11
 *
 * 基于 [Flowable] 使用 [PublishProcessor] 来发事件
 * 普通事件使用 PublishProcessor: 只接收订阅之后的数据
 * 粘性事件使用 BehaviorSubject: 会接收订阅之后
 *
 */

class EventBus private constructor() {

    private val processorMap: LruCache<Class<*>, FlowableProcessor<*>> = LruCache(maxSize)

    companion object {
        private const val TAG = "EventBus"

        @JvmStatic
        val instance: EventBus
            get() = Loader.INSTANCE

        @JvmStatic
        private var maxSize = 100

        @JvmStatic
        fun setMaxSize(maxSize: Int) {
            this.maxSize = maxSize
        }
    }

    private object Loader {
        val INSTANCE = EventBus()
    }

    /**
     * @param clazz 为了类型安全, 指定事件 type class
     */
    fun <T> on(clazz: Class<T>): Bus<T> {
        return Bus(clazz, processorMap)
    }

    class Bus<S>(
        private val clazz: Class<S>,
        private val processorMap: LruCache<Class<*>, FlowableProcessor<*>>
    ) {

        private fun ifProcessorMapGetNull(): FlowableProcessor<S> {
            val processor = PublishProcessor.create<S>()
            processorMap.put(clazz, processor)
            return processor
        }

        /**
         * observe a event
         *
         * @param sticky whether ths event is a sticky event
         * @param callback callback after observing the event
         */
        @MainThread
        @Suppress("UNCHECKED_CAST")
        fun observe(
            lifecycleOwner: LifecycleOwner,
            callback: EventCallback<S>
        ) {
            val processor =
                ((processorMap[clazz] ?: ifProcessorMapGetNull()) as FlowableProcessor<S>)
            processor.observeOn(
                AndroidSchedulers.mainThread()
            ).to(
                AutoDispose.autoDisposable(
                    AndroidLifecycleScopeProvider.from(
                        lifecycleOwner, Lifecycle.Event.ON_DESTROY
                    )
                )
            ).subscribe({
                callback(it)
            }, {
                // todo 出错后后面的事件都收不到了
                Log.d(TAG, "$it")
            })


        }
    }

    @Suppress("UNCHECKED_CAST")
    fun post(event: Any, sticky: Boolean = false) {
        if (sticky) {
            if (processorMap[event::class.java] == null) {
                processorMap.put(event::class.java, BehaviorProcessor.create<Any>())
            }
        }
        ((processorMap[event::class.java] ?: return) as? FlowableProcessor<Any>)?.run {
            when (this) {
                is PublishProcessor -> {
                    offer(event)
                }
                is BehaviorProcessor -> {
                    offer(event)
                }
                else -> {

                }
            }
        }
    }

    fun postSticky(event: Any) = post(event, true)
}
typealias EventCallback<T> = (T) -> Unit


