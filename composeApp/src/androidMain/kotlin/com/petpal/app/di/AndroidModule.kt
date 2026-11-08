package com.petpal.app.di

import com.petpal.app.data.databaseBuilder
import com.petpal.app.data.local.buildDatabase
import com.petpal.app.domain.platform.StepCounter
import com.petpal.app.platform.AndroidStepCounter
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single { buildDatabase(databaseBuilder(androidContext())) }
    single<StepCounter> { AndroidStepCounter(androidContext()) }
}
