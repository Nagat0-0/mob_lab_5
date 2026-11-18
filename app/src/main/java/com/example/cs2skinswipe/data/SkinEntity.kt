package com.example.cs2skinswipe.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_skins")
data class SkinEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val weaponName: String,
    val category: String,
    val rarityColor: String,
    val imageUrl: String,
    val description: String,
    var sortIndex: Int
)