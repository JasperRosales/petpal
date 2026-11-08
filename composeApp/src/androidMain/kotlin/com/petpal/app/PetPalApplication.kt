package com.petpal.app

import android.app.Application
import com.petpal.app.di.androidModule
import com.petpal.app.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PetPalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PetPalApplication)
            modules(sharedModule, androidModule)
        }
    }
}
