package com.lwjlol.eventbus

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.uber.autodispose.AutoDispose
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.processors.PublishProcessor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * @author luwenjie on 2019-07-05 18:11:11
 *
 * 基于 [Flowable] 使用 [PublishProcessor] 来发事件
 */

class EventBus private constructor() {

    private val processorMap: ConcurrentMap<Class<*>, PublishProcessor<*>> = ConcurrentHashMap()
    private val stickyEventsList: CopyOnWriteArrayList<Any> = CopyOnWriteArrayList()

    companion object {
        private const val TAG = "EventBus"

        @JvmStatic
        val instance: EventBus
            get() = Loader.INSTANCE
    }

    private object Loader {
        val INSTANCE = EventBus()
    }

    /**
     * @param clazz 为了类型安全, 指定事件 type class
     */
    fun <T> on(clazz: Class<T>): Bus<T> {
        return Bus(clazz, processorMap, stickyEventsList)
    }

    class Bus<S>(
        private val clazz: Class<S>,
        private val processorMap: ConcurrentMap<Class<*>, PublishProcessor<*>>,
        private val stickyEventsList: CopyOnWriteArrayList<Any>
    ) {

        private fun ifProcessorMapGetNull(): PublishProcessor<S> {
            val processor = PublishProcessor.create<S>()
            processorMap[clazz] = processor
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
            lifecycleOwner: LifecycleOwner, sticky: Boolean = false,
            callback: EventCallback<S>
        ) {
            fun subscribe(processor: PublishProcessor<S>) {
                processor.observeOn(AndroidSchedulers.mainThread()).`as`(
                    AutoDispose.autoDisposable(
                        AndroidLifecycleScopeProvider.from(
                            lifecycleOwner,
                            Lifecycle.Event.ON_DESTROY
                        )
                    )
                ).subscribe({
                    callback(it)
                }, {
                    Log.d(TAG, "$it")
                })
            }
            if (sticky) {
                if (stickyEventsList.isEmpty()) {
                    return
                }
                val processor = PublishProcessor.create<S>()
                subscribe(processor)

                for (i in stickyEventsList.indices) {
                    if (clazz == stickyEventsList[i]::class.java) {
                        val stickyEvent = stickyEventsList[i] as S
                        processor.offer(stickyEvent)
                        stickyEventsList.removeAt(i)
                        break
                    }
                }
            } else {
                subscribe((processorMap[clazz] ?: ifProcessorMapGetNull()) as PublishProcessor<S>)
            }

        }
    }

    @Suppress("UNCHECKED_CAST")
    fun post(event: Any, sticky: Boolean = false) {
        if (sticky) {
            if (!stickyEventsList.contains(event)) {
                stickyEventsList.add(event)
            }
        } else {
            ((processorMap[event::class.java] ?: return) as? PublishProcessor<Any>)?.run {
                offer(event)
            }
        }
    }
}
typealias EventCallback<T> = (T) -> Unit


