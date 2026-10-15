package com.asim.jhbatteryapp

import android.app.Application

class JHBatteryApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: JHBatteryApplication
            private set
    }
}
