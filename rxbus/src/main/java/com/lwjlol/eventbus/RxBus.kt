package com.lwjlol.eventbus

import androidx.annotation.CheckResult
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import kotlin.reflect.KClass

/**
 * 使用 RxRelay 处理异常后订阅断掉的情况
 */
object RxBus {
    private val relay = PublishRelay.create<Any>().toSerialized()
    private val stickyRelay = BehaviorRelay.create<Any>().toSerialized()

    fun post(event: Any) = relay.accept(event)

    fun postSticky(event: Any) = stickyRelay.accept(event)

    @CheckResult
    fun <T> event(eventType: Class<T>): Flowable<T> {
        return relay.ofType(eventType).toFlowable(BackpressureStrategy.BUFFER)
    }

    @CheckResult
    fun <T> eventSticky(eventType: Class<T>): Flowable<T> {
        return stickyRelay.ofType(eventType).toFlowable(BackpressureStrategy.BUFFER)
    }

    @CheckResult
    fun <T : Any> event(eventType: KClass<T>): Flowable<T> = event(eventType.java)

    @CheckResult
    fun <T : Any> eventSticky(eventType: KClass<T>): Flowable<T> = eventSticky(eventType.java)

    @CheckResult
    fun hasObservers(): Boolean = relay.hasObservers()


    @CheckResult
    fun hasStickyObservers(): Boolean = stickyRelay.hasObservers()

}