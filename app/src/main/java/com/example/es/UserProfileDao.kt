package com.example.es

import androidx.room.*

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE uid = :uid LIMIT 1")
    fun getProfile(uid: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProfile(profile: UserProfile)

    @Query("UPDATE user_profile SET profileImageUrl = :url WHERE uid = :uid")
    fun updateProfileImage(uid: String, url: String)
}
