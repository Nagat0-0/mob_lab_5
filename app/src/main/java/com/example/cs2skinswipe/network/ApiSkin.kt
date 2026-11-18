package com.example.cs2skinswipe.network

data class ApiSkin(
    val id: String,
    val name: String,
    val description: String?,
    val weapon: ApiNamedItem?,
    val category: ApiNamedItem?,
    val rarity: ApiRarity?,
    val image: String?
)

data class ApiNamedItem(
    val id: String?,
    val name: String?
)

data class ApiRarity(
    val id: String?,
    val name: String?,
    val color: String?
)