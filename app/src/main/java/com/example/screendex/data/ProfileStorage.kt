package com.example.screendex.data

import android.content.Context
import com.example.screendex.UserProfile

class ProfileStorage(context: Context) {
    private val preferences = context.getSharedPreferences(
        "screendex_profile",
        Context.MODE_PRIVATE
    )

    fun load(): UserProfile {
        return UserProfile(
            name = preferences.getString(KEY_NAME, null) ?: "ScreenDex User",
            email = preferences.getString(KEY_EMAIL, null) ?: "warren@email.com"
        )
    }

    fun save(profile: UserProfile) {
        preferences.edit()
            .putString(KEY_NAME, profile.name)
            .putString(KEY_EMAIL, profile.email)
            .apply()
    }

    companion object {
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
    }
}