package com.autoclicker.pro.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Everything currently injected (CoordinateManager, ClickExecutor,
 * AutoClickEngine, ProfileRepository, SettingsRepository) uses
 * @Inject constructor directly, so no @Provides bindings are needed yet.
 * This module is kept as the place to add them (e.g. if Room is introduced
 * later) without hunting for where DI wiring lives.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
