package com.example.es

import androidx.room.*

@Dao
interface DebitCardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDebitCard(card: DebitCard)

    @Query("SELECT * FROM debit_cards ORDER BY orderIndex ASC")
    fun getAllDebitCards(): List<DebitCard>

    @Query("SELECT * FROM debit_cards WHERE id = :id")
    fun getDebitCardById(id: Int): DebitCard?

    @Update
    fun updateDebitCard(card: DebitCard)

    @Delete
    fun deleteDebitCard(card: DebitCard)
}
