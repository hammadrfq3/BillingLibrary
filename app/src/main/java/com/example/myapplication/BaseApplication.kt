package com.example.myapplication

import android.app.Application

class BaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @JvmStatic
       lateinit var instance: BaseApplication
    }

}