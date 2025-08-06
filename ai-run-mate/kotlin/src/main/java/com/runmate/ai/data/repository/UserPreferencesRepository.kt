package com.runmate.ai.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.runmate.ai.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 用户偏好设置仓库
 */
class UserPreferencesRepository(private val context: Context) {
    
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")
    
    companion object {
        private val AI_VOICE_GENDER = stringPreferencesKey("ai_voice_gender")
        private val BROADCAST_INTERVAL = stringPreferencesKey("broadcast_interval")
        private val DISTANCE_BROADCAST_INTERVAL = doublePreferencesKey("distance_broadcast_interval")
        private val ENABLE_PACE_ALERTS = booleanPreferencesKey("enable_pace_alerts")
        private val PACE_ALERT_THRESHOLD = doublePreferencesKey("pace_alert_threshold")
        private val ENABLE_ENCOURAGEMENT = booleanPreferencesKey("enable_encouragement")
        private val ENABLE_PROFESSIONAL_ADVICE = booleanPreferencesKey("enable_professional_advice")
        private val DEFAULT_TARGET_DISTANCE = doublePreferencesKey("default_target_distance")
        private val DEFAULT_TARGET_DURATION = longPreferencesKey("default_target_duration")
        private val UNIT_SYSTEM = stringPreferencesKey("unit_system")
    }
    
    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            aiVoiceGender = VoiceGender.valueOf(
                preferences[AI_VOICE_GENDER] ?: VoiceGender.FEMALE.name
            ),
            broadcastInterval = BroadcastInterval.valueOf(
                preferences[BROADCAST_INTERVAL] ?: BroadcastInterval.EVERY_2_MINUTES.name
            ),
            distanceBroadcastInterval = preferences[DISTANCE_BROADCAST_INTERVAL] ?: 1.0,
            enablePaceAlerts = preferences[ENABLE_PACE_ALERTS] ?: true,
            paceAlertThreshold = preferences[PACE_ALERT_THRESHOLD] ?: 30.0,
            enableEncouragement = preferences[ENABLE_ENCOURAGEMENT] ?: true,
            enableProfessionalAdvice = preferences[ENABLE_PROFESSIONAL_ADVICE] ?: true,
            defaultTargetDistance = preferences[DEFAULT_TARGET_DISTANCE],
            defaultTargetDuration = preferences[DEFAULT_TARGET_DURATION],
            unitSystem = UnitSystem.valueOf(
                preferences[UNIT_SYSTEM] ?: UnitSystem.METRIC.name
            )
        )
    }
    
    suspend fun updateAiVoiceGender(gender: VoiceGender) {
        context.dataStore.edit { preferences ->
            preferences[AI_VOICE_GENDER] = gender.name
        }
    }
    
    suspend fun updateBroadcastInterval(interval: BroadcastInterval) {
        context.dataStore.edit { preferences ->
            preferences[BROADCAST_INTERVAL] = interval.name
        }
    }
    
    suspend fun updateDistanceBroadcastInterval(interval: Double) {
        context.dataStore.edit { preferences ->
            preferences[DISTANCE_BROADCAST_INTERVAL] = interval
        }
    }
    
    suspend fun updatePaceAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_PACE_ALERTS] = enabled
        }
    }
    
    suspend fun updatePaceAlertThreshold(threshold: Double) {
        context.dataStore.edit { preferences ->
            preferences[PACE_ALERT_THRESHOLD] = threshold
        }
    }
    
    suspend fun updateEncouragementEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_ENCOURAGEMENT] = enabled
        }
    }
    
    suspend fun updateProfessionalAdviceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_PROFESSIONAL_ADVICE] = enabled
        }
    }
    
    suspend fun updateDefaultTargetDistance(distance: Double?) {
        context.dataStore.edit { preferences ->
            if (distance != null) {
                preferences[DEFAULT_TARGET_DISTANCE] = distance
            } else {
                preferences.remove(DEFAULT_TARGET_DISTANCE)
            }
        }
    }
    
    suspend fun updateDefaultTargetDuration(duration: Long?) {
        context.dataStore.edit { preferences ->
            if (duration != null) {
                preferences[DEFAULT_TARGET_DURATION] = duration
            } else {
                preferences.remove(DEFAULT_TARGET_DURATION)
            }
        }
    }
    
    suspend fun updateUnitSystem(unitSystem: UnitSystem) {
        context.dataStore.edit { preferences ->
            preferences[UNIT_SYSTEM] = unitSystem.name
        }
    }
}