package com.lwjlol.eventbus.sample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.lwjlol.eventbus.EventBus

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        EventBus.setMaxSize(3)
//        EventBus.instance.on(String::class.java).observe(this) {
//            Log.d(TAG, it)
//        }

        EventBus.instance.postSticky(EventA("123123123"))


        EventBus.instance.on(EventA::class.java).observe(this) {
            Log.d(TAG, "${it.s}")
        }

        EventBus.instance.post(EventA("222222222"))

    }
}


data class EventA(val s:String)