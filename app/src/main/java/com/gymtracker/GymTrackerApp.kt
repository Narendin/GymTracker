package com.gymtracker

import android.app.Application
import com.gymtracker.di.AppContainer

class GymTrackerApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
