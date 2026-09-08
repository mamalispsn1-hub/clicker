package com.autoclicker.pro.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Profile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Default",
    val sequence: ClickSequence = ClickSequence(),
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class ProfileCollection(
    val profiles: List<Profile> = listOf(Profile(name = "Default")),
    val activeProfileId: String = profiles.first().id
)
