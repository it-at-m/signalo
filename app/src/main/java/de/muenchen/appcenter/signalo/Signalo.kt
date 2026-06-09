package de.muenchen.appcenter.signalo

import android.app.Application
import timber.log.Timber

class Signalo : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(CustomDebugTree())
    }
}