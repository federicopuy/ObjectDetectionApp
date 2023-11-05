package com.federicopuy.objectdetectionapp.di

import android.content.Context
import androidx.camera.core.ExperimentalGetImage
import com.federicopuy.objectdetectionapp.objectdetection.pytorch.PytorchObjectDetector
import com.federicopuy.objectdetectionapp.objectdetection.tflite.TFLiteObjectDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@ExperimentalGetImage
@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {


    @Provides
    @Singleton
    fun provideTFLiteObjectDetector(@ApplicationContext appContext: Context): TFLiteObjectDetector {
        return TFLiteObjectDetector(context = appContext)
    }

    @Provides
    @Singleton
    fun providePyTorchObjectDetector(@ApplicationContext appContext: Context): PytorchObjectDetector {
        return PytorchObjectDetector(context = appContext)
    }

}