package com.example.arcadia.di

import com.example.arcadia.data.GamerRepositoryImpl
import com.example.arcadia.data.repository.GameRepositoryImpl
import com.example.arcadia.data.repository.UserGamesRepositoryImpl
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.domain.repository.UserGamesRepository
import org.koin.dsl.module

val repositoryModule = module {
    single<GamerRepository> { GamerRepositoryImpl() }
    single<GameRepository> { GameRepositoryImpl(get()) }
    single<UserGamesRepository> { UserGamesRepositoryImpl() }
}