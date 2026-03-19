package com.example.es

import androidx.room.*

@Dao
interface CreditCardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCreditCard(card: CreditCard)

    @Query("SELECT * FROM credit_cards ORDER BY orderIndex ASC")
    fun getAllCreditCards(): List<CreditCard>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    fun getCreditCardById(id: Int): CreditCard?

    @Update
    fun updateCreditCard(card: CreditCard)

    @Delete
    fun deleteCreditCard(card: CreditCard)
}
