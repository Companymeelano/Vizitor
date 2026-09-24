package ir.g1z4.controlpro.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.g1z4.controlpro.communication.AndroidSmsSender
import ir.g1z4.controlpro.communication.SmsSender
import ir.g1z4.controlpro.data.db.AppDatabase
import ir.g1z4.controlpro.data.settings.SettingsStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "g1z4.db").build()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindModule {
    @Binds
    abstract fun smsSender(impl: AndroidSmsSender): SmsSender
}

@dagger.hilt.EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun database(): AppDatabase
    fun settings(): SettingsStore
}
