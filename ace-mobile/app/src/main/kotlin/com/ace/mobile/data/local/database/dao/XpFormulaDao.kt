package com.ace.mobile.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ace.mobile.data.local.database.entity.LocalXpFormulaEntity

@Dao
interface XpFormulaDao {

    @Query("SELECT * FROM xp_formulas WHERE sportType = :sportType LIMIT 1")
    suspend fun getFormula(sportType: String): LocalXpFormulaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFormula(formula: LocalXpFormulaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(formulas: List<LocalXpFormulaEntity>)

    @Query("DELETE FROM xp_formulas")
    suspend fun clearAll()

    @Query("SELECT * FROM xp_formulas")
    suspend fun getAll(): List<LocalXpFormulaEntity>
}