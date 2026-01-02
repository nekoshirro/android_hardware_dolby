/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.data

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import org.lunaris.dolby.DolbyConstants
import org.lunaris.dolby.DolbyConstants.DsParam
import org.lunaris.dolby.R
import org.lunaris.dolby.audio.DolbyAudioEffect
import org.lunaris.dolby.domain.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DolbyRepository(private val context: Context) {

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var dolbyEffect = DolbyAudioEffect(EFFECT_PRIORITY, audioSession = 0)
    
    private val defaultPrefs = context.getSharedPreferences("dolby_prefs", Context.MODE_PRIVATE)
    private val presetsPrefs = context.getSharedPreferences(DolbyConstants.PREF_FILE_PRESETS, Context.MODE_PRIVATE)
    
    private val _isOnSpeaker = MutableStateFlow(checkIsOnSpeaker())
    val isOnSpeaker: StateFlow<Boolean> = _isOnSpeaker.asStateFlow()

    val stereoWideningSupported = context.resources.getBoolean(R.bool.dolby_stereo_widening_supported)
    val volumeLevelerSupported = context.resources.getBoolean(R.bool.dolby_volume_leveler_supported)

    private fun checkEffect() {
        if (!dolbyEffect.hasControl()) {
            dolbyEffect.release()
            dolbyEffect = DolbyAudioEffect(EFFECT_PRIORITY, audioSession = 0)
        }
    }

    private fun checkIsOnSpeaker(): Boolean {
        val device = audioManager.getDevicesForAttributes(ATTRIBUTES_MEDIA)[0]
        return device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
    }

    fun updateSpeakerState() {
        _isOnSpeaker.value = checkIsOnSpeaker()
    }

    fun getDolbyEnabled(): Boolean {
        return dolbyEffect.dsOn
    }

    fun setDolbyEnabled(enabled: Boolean) {
        checkEffect()
        dolbyEffect.dsOn = enabled
        defaultPrefs.edit().putBoolean(DolbyConstants.PREF_ENABLE, enabled).apply()
    }

    fun getCurrentProfile(): Int {
        return dolbyEffect.profile
    }

    fun setCurrentProfile(profile: Int) {
        checkEffect()
        dolbyEffect.profile = profile
        defaultPrefs.edit().putString(DolbyConstants.PREF_PROFILE, profile.toString()).apply()
    }

    private fun getProfilePrefs(profile: Int): SharedPreferences {
        return context.getSharedPreferences("profile_$profile", Context.MODE_PRIVATE)
    }

    fun getBassEnhancerEnabled(profile: Int): Boolean {
        return dolbyEffect.getDapParameterBool(DsParam.BASS_ENHANCER_ENABLE, profile)
    }

    fun setBassEnhancerEnabled(profile: Int, enabled: Boolean) {
        checkEffect()
        dolbyEffect.setDapParameter(DsParam.BASS_ENHANCER_ENABLE, enabled, profile)
        getProfilePrefs(profile).edit().putBoolean(DolbyConstants.PREF_BASS, enabled).apply()
    }

    fun getVolumeLevelerEnabled(profile: Int): Boolean {
        return dolbyEffect.getDapParameterBool(DsParam.VOLUME_LEVELER_ENABLE, profile)
    }

    fun setVolumeLevelerEnabled(profile: Int, enabled: Boolean) {
        checkEffect()
        dolbyEffect.setDapParameter(DsParam.VOLUME_LEVELER_ENABLE, enabled, profile)
        getProfilePrefs(profile).edit().putBoolean(DolbyConstants.PREF_VOLUME, enabled).apply()
    }

    fun getIeqPreset(profile: Int): Int {
        return dolbyEffect.getDapParameterInt(DsParam.IEQ_PRESET, profile)
    }

    fun setIeqPreset(profile: Int, preset: Int) {
        checkEffect()
        dolbyEffect.setDapParameter(DsParam.IEQ_PRESET, preset, profile)
        getProfilePrefs(profile).edit().putString(DolbyConstants.PREF_IEQ, preset.toString()).apply()
    }

    fun getHeadphoneVirtualizerEnabled(profile: Int): Boolean {
        return dolbyEffect.getDapParameterBool(DsParam.HEADPHONE_VIRTUALIZER, profile)
    }

    fun setHeadphoneVirtualizerEnabled(profile: Int, enabled: Boolean) {
        checkEffect()
        dolbyEffect.setDapParameter(DsParam.HEADPHONE_VIRTUALIZER, enabled, profile)
        getProfilePrefs(profile).edit().putBoolean(DolbyConstants.PREF_HP_VIRTUALIZER, enabled).apply()
    }

    fun getSpeakerVirtualizerEnabled(profile: Int): Boolean {
        return dolbyEffect.getDapParameterBool(DsParam.SPEAKER_VIRTUALIZER, profile)
    }

    fun setSpeakerVirtualizerEnabled(profile: Int, enabled: Boolean) {
        checkEffect()
        dolbyEffect.setDapParameter(DsParam.SPEAKER_VIRTUALIZER, enabled, profile)
        getProfilePrefs(profile).edit().putBoolean(DolbyConstants.PREF_SPK_VIRTUALIZER, enabled).apply()
    }

    fun getStereoWideningAmount(profile: Int): Int {
        if (!stereoWideningSupported) return 0
        return dolbyEffect.getDapParameterInt(DsParam.STEREO_WIDENING_AMOUNT, profile)
    }

    fun setStereoWideningAmount(profile: Int, amount: Int) {
        if (!stereoWideningSupported) return
        checkEffect()
        dolbyEffect.setDapParameter(DsParam.STEREO_WIDENING_AMOUNT, amount, profile)
        getProfilePrefs(profile).edit().putInt(DolbyConstants.PREF_STEREO_WIDENING, amount).apply()
    }

    fun getDialogueEnhancerEnabled(profile: Int): Boolean {
        return dolbyEffect.getDapParameterBool(DsParam.DIALOGUE_ENHANCER_ENABLE, profile)
    }

    fun setDialogueEnhancerEnabled(profile: Int, enabled: Boolean) {
        checkEffect()
        dolbyEffect.setDapParameter(DsParam.DIALOGUE_ENHANCER_ENABLE, enabled, profile)
        getProfilePrefs(profile).edit().putBoolean(DolbyConstants.PREF_DIALOGUE, enabled).apply()
    }

    fun getDialogueEnhancerAmount(profile: Int): Int {
        return dolbyEffect.getDapParameterInt(DsParam.DIALOGUE_ENHANCER_AMOUNT, profile)
    }

    fun setDialogueEnhancerAmount(profile: Int, amount: Int) {
        checkEffect()
        dolbyEffect.setDapParameter(DsParam.DIALOGUE_ENHANCER_AMOUNT, amount, profile)
        getProfilePrefs(profile).edit().putInt(DolbyConstants.PREF_DIALOGUE_AMOUNT, amount).apply()
    }

    fun getEqualizerGains(profile: Int): List<BandGain> {
        val gains = dolbyEffect.getDapParameter(DsParam.GEQ_BAND_GAINS, profile)
        return deserializeGains(gains)
    }

    fun setEqualizerGains(profile: Int, bandGains: List<BandGain>) {
        checkEffect()
        val gains = serializeGains(bandGains)
        dolbyEffect.setDapParameter(DsParam.GEQ_BAND_GAINS, gains, profile)
        val gainsString = gains.joinToString(",")
        getProfilePrefs(profile).edit().putString(DolbyConstants.PREF_PRESET, gainsString).apply()
    }

    fun getPresetName(profile: Int): String {
        val gains = dolbyEffect.getDapParameter(DsParam.GEQ_BAND_GAINS, profile)
        val gainsString = gains.joinToString(",")
        
        val presetValues = context.resources.getStringArray(R.array.dolby_preset_values)
        val presetNames = context.resources.getStringArray(R.array.dolby_preset_entries)
        presetValues.forEachIndexed { index, preset ->
            if (gainsToCompareFormat(preset) == gainsToCompareFormat(gainsString)) {
                return presetNames[index]
            }
        }
        
        presetsPrefs.all.forEach { (name, value) ->
            if (gainsToCompareFormat(value.toString()) == gainsToCompareFormat(gainsString)) {
                return name
            }
        }
        
        return context.getString(R.string.dolby_preset_custom)
    }

    private fun gainsToCompareFormat(gains: String): String {
        return gains.split(",").map { it.toIntOrNull() ?: 0 }.joinToString(",")
    }

    fun getUserPresets(): List<EqualizerPreset> {
        return presetsPrefs.all.map { (name, value) ->
            EqualizerPreset(
                name = name,
                bandGains = deserializeGains(value.toString().split(",").map { it.toInt() }.toIntArray()),
                isUserDefined = true
            )
        }
    }

    fun addUserPreset(name: String, bandGains: List<BandGain>) {
        val gains = serializeGains(bandGains).joinToString(",")
        presetsPrefs.edit().putString(name, gains).apply()
    }

    fun deleteUserPreset(name: String) {
        presetsPrefs.edit().remove(name).apply()
    }

    fun resetProfile(profile: Int) {
        checkEffect()
        dolbyEffect.resetProfileSpecificSettings(profile)
        context.deleteSharedPreferences("profile_$profile")
    }

    fun resetAllProfiles() {
        checkEffect()
        context.resources.getStringArray(R.array.dolby_profile_values)
            .map { it.toInt() }
            .forEach { resetProfile(it) }
        setCurrentProfile(0)
    }

    private fun deserializeGains(gains: IntArray): List<BandGain> {
        val tenBandGains = gains.filterIndexed { index, _ -> index % 2 == 0 }
        return BAND_FREQUENCIES.mapIndexed { index, freq ->
            BandGain(frequency = freq, gain = tenBandGains.getOrElse(index) { 0 })
        }
    }

    private fun deserializeGains(gainsString: String): List<BandGain> {
        val gains = gainsString.split(",").map { it.toIntOrNull() ?: 0 }
        val tenBandGains = if (gains.size == 20) {
            gains.filterIndexed { index, _ -> index % 2 == 0 }
        } else {
            gains
        }
        return BAND_FREQUENCIES.mapIndexed { index, freq ->
            BandGain(frequency = freq, gain = tenBandGains.getOrElse(index) { 0 })
        }
    }

    private fun serializeGains(bandGains: List<BandGain>): IntArray {
        val tenBands = bandGains.map { it.gain }
        return IntArray(20) { index ->
            if (index % 2 == 1 && index < 19) {
                (tenBands[(index - 1) / 2] + tenBands[(index + 1) / 2]) / 2
            } else {
                tenBands[index / 2]
            }
        }
    }

    companion object {
        private const val TAG = "DolbyRepository"
        private const val EFFECT_PRIORITY = 100
        
        private val ATTRIBUTES_MEDIA = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        val BAND_FREQUENCIES = listOf(32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
    }
}
