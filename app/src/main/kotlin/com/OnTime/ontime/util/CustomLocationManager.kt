package com.OnTime.ontime.util

import com.OnTime.ontime.data.repositories.SettingsRepository

class CustomLocationManager(private val settingsRepository: SettingsRepository) {

    /**
     * Fetches custom locations from the repository and finds a matching one in the event title.
     *
     * @param title The event title.
     * @return The mapped address if a keyword is found, otherwise null.
     */
    suspend fun findLocationFromTitle(title: String): String? {
        val userLocations = settingsRepository.getCustomLocations()
        if (userLocations.isEmpty()) {
            return null
        }

        // Find the first location whose name (keyword) is present in the title
        return userLocations.find { location ->
            title.contains(location.name, ignoreCase = true)
        }?.address
    }
}
