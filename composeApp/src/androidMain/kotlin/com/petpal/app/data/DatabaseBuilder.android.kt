package com.petpal.app.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.petpal.app.data.local.PetPalDatabase

fun databaseBuilder(context: Context): RoomDatabase.Builder<PetPalDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("petpal.db")
    return Room.databaseBuilder<PetPalDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}
