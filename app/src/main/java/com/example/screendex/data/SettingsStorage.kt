package com.example.screendex.data

import android.content.Context
import com.example.screendex.UserSettings

class SettingsStorage(context: Context) {
    private val preferences = context.getSharedPreferences(
        "screendex_settings",
        Context.MODE_PRIVATE
    )

    fun load(): UserSettings {
        return UserSettings(
            newReleaseNotifications = preferences.getBoolean(KEY_NEW_RELEASES, true),
            weeklyEmails = preferences.getBoolean(KEY_WEEKLY_EMAILS, false)
        )
    }

    fun save(settings: UserSettings) {
        preferences.edit()
            .putBoolean(KEY_NEW_RELEASES, settings.newReleaseNotifications)
            .putBoolean(KEY_WEEKLY_EMAILS, settings.weeklyEmails)
            .apply()
    }

    companion object {
        private const val KEY_NEW_RELEASES = "new_release_notifications"
        private const val KEY_WEEKLY_EMAILS = "weekly_emails"
    }
}