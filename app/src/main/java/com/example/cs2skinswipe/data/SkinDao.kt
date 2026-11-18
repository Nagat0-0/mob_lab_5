package com.example.cs2skinswipe.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class NameCount(
    val name: String,
    val count: Int
)

@Dao
interface SkinDao {
    @Query("SELECT category AS name, COUNT(*) AS count FROM saved_skins GROUP BY category ORDER BY category ASC")
    fun getSavedCategories(): Flow<List<NameCount>>

    @Query("SELECT weaponName AS name, COUNT(*) AS count FROM saved_skins WHERE category = :category GROUP BY weaponName ORDER BY weaponName ASC")
    fun getWeaponsByCategory(category: String): Flow<List<NameCount>>

    @Query("SELECT * FROM saved_skins WHERE weaponName = :weaponName ORDER BY sortIndex ASC")
    fun getSkinsByWeapon(weaponName: String): Flow<List<SkinEntity>>

    @Query("SELECT * FROM saved_skins WHERE category = :category ORDER BY sortIndex ASC")
    fun getSkinsByCategory(category: String): Flow<List<SkinEntity>>

    @Query("SELECT id FROM saved_skins")
    suspend fun getAllSavedIds(): List<String>

    @Query("SELECT COUNT(*) FROM saved_skins WHERE weaponName = :weaponName")
    suspend fun getSkinCountForWeapon(weaponName: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkin(skin: SkinEntity)

    @Update
    suspend fun updateSkins(skins: List<SkinEntity>)

    @Query("DELETE FROM saved_skins WHERE id = :id")
    suspend fun deleteSkin(id: String)
}