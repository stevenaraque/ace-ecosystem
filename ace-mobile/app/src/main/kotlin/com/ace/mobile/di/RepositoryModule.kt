package com.ace.mobile.di

import com.ace.mobile.data.repository.SessionRepository
import com.ace.mobile.data.repository.SessionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Módulo Hilt que vincula interfaces de repositorio con sus implementaciones.
 *
 * Sistema 2 — Sesión de Ejercicio (Apéndice S2)
 *
 * @see SessionRepository
 * @see SessionRepositoryImpl
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Vincula [SessionRepository] → [SessionRepositoryImpl].
     *
     * @param impl Instancia de [SessionRepositoryImpl] inyectada por Hilt.
     * @return La interfaz [SessionRepository] vinculada a la implementación.
     */
    @Binds
    abstract fun bindSessionRepository(
        impl: SessionRepositoryImpl
    ): SessionRepository
}