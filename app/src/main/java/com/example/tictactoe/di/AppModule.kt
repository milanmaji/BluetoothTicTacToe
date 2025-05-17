package com.example.tictactoe.di

import android.content.Context
import com.example.tictactoe.data.AndroidBluetoothController
import com.example.tictactoe.data.AndroidBluetoothServerController
import com.example.tictactoe.domain.BluetoothController
import com.example.tictactoe.domain.BluetoothServerController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(ViewModelComponent::class)
object AppModule {

    @Provides
    @ViewModelScoped
    fun provideBluetoothController(@ApplicationContext context: Context): BluetoothController {
        return AndroidBluetoothController(context)
    }

    @Provides
    @ViewModelScoped
    fun provideBluetoothServerController(@ApplicationContext context: Context): BluetoothServerController {
        return AndroidBluetoothServerController(context)
    }
}