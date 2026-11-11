package com.petpal.app.di

import com.petpal.app.data.databaseBuilder
import com.petpal.app.data.local.buildDatabase
import com.petpal.app.domain.platform.BattleLinks
import com.petpal.app.domain.platform.StepCounter
import com.petpal.app.domain.platform.SoundPlayer
import com.petpal.app.platform.sound.AndroidSoundPlayer
import com.petpal.app.platform.link.LanTransport
import com.petpal.app.platform.AndroidStepCounter
import com.petpal.app.platform.AndroidAppUpdater
import com.petpal.app.domain.platform.AppUpdater
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single { buildDatabase(databaseBuilder(androidContext())) }
    single<StepCounter> { AndroidStepCounter(androidContext()) }
    single<com.petpal.app.domain.platform.StepTracking> { com.petpal.app.platform.AndroidStepTracking(androidContext()) }
    single<SoundPlayer> { AndroidSoundPlayer(androidContext()) }
    single<AppUpdater> { AndroidAppUpdater(androidContext()) }
    single<com.petpal.app.domain.platform.OnlineService> { com.petpal.app.platform.link.OnlineClient(androidContext()) }
    single { BattleLinks(listOf(LanTransport(androidContext()))) }
}
