package com.example.arcadia.domain.model

import kotlinx.serialization.Serializable


@Serializable
data class Gamer(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val username: String = "",
    val country: String? = null,
    val city: String? = null,
    val gender: String? = null,
    val description: String? = "",
    val profileComplete: Boolean = false,

)
