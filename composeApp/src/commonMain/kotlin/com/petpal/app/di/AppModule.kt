package com.petpal.app.di

import com.petpal.app.data.local.PetPalDatabase
import com.petpal.app.data.repository.*
import com.petpal.app.domain.platform.TimeProvider
import com.petpal.app.ui.focus.FocusSession
import com.petpal.app.ui.focus.FocusViewModel
import com.petpal.app.ui.habits.HabitsViewModel
import com.petpal.app.ui.home.HomeViewModel
import com.petpal.app.ui.stats.StatsViewModel
import kotlinx.datetime.Clock
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sharedModule = module {
    single<TimeProvider> { TimeProvider { Clock.System.now().toEpochMilliseconds() } }
    single { get<PetPalDatabase>().petDao() }
    single { get<PetPalDatabase>().habitLogDao() }
    single { get<PetPalDatabase>().stepDayDao() }

    single { PetRepository(get(), get(), get()) }
    single { HabitRepository(get(), get()) }
    single { StepRepository(get(), get(), get(), get()) }
    single { FocusSession(get(), get()) }

    viewModel { HomeViewModel(get(), get(), get(), get()) }
    viewModel { HabitsViewModel(get(), get()) }
    viewModel { FocusViewModel(get()) }
    viewModel { StatsViewModel(get(), get()) }
}
