package com.example.myapplication.model.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.weatherapp.model.pojos.CurrentWeatherResponse
import com.example.weatherapp.model.pojos.WeatherData
import com.example.weatherapp.model.pojos.WeatherResponse

@Database(
    entities = [CurrentWeatherResponse::class, WeatherResponse::class,
    FavoritePlace::class,
    Alert::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "weather_database"
                )
                    //.addMigrations(MIGRATION_1_2, MIGRATION_2_3) // Removed migrations
                    .fallbackToDestructiveMigration() // Optional: Wipe and recreate DB if schema changed
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
