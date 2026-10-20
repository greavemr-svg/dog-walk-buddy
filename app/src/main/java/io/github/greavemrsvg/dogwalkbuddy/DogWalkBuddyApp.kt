package io.github.greavemrsvg.dogwalkbuddy

import android.app.Application

class DogWalkBuddyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.createDefault()
    }
}
