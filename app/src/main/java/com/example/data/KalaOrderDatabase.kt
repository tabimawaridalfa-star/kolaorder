package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MenuItemEntity::class,
        OrderEntity::class,
        BotSettingsEntity::class,
        BotContactStateEntity::class,
        BotActivityLogEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class KalaOrderDatabase : RoomDatabase() {
    abstract fun menuItemDao(): MenuItemDao
    abstract fun orderDao(): OrderDao
    abstract fun botSettingsDao(): BotSettingsDao
    abstract fun botContactStateDao(): BotContactStateDao
    abstract fun botActivityLogDao(): BotActivityLogDao

    companion object {
        @Volatile
        private var INSTANCE: KalaOrderDatabase? = null

        fun getInstance(context: Context): KalaOrderDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KalaOrderDatabase::class.java,
                    "kalaorder_kasir.db",
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
