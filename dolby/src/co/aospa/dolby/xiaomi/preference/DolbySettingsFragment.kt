/*
 * Copyright (C) 2023-25 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.preference

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemProperties
import android.view.View
import android.widget.CompoundButton
import android.widget.Toast
import androidx.core.os.postDelayed
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import co.aospa.dolby.xiaomi.DolbyConstants
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_BASS
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_DIALOGUE
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_DIALOGUE_AMOUNT
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_ENABLE
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_HP_VIRTUALIZER
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_IEQ
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_PRESET
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_PROFILE
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_RESET
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_SPK_VIRTUALIZER
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_STEREO_WIDENING
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_VOLUME
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.dlog
import co.aospa.dolby.xiaomi.DolbyController
import co.aospa.dolby.xiaomi.R
import com.android.settingslib.widget.MainSwitchPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment

class DolbySettingsFragment : SettingsBasePreferenceFragment(),
    Preference.OnPreferenceChangeListener, CompoundButton.OnCheckedChangeListener {

    private val appContext: Context
        get() = requireContext().applicationContext

    private val switchBar by lazy { findPreference<MainSwitchPreference>(PREF_ENABLE)!! }
    private val profilePref by lazy { findPreference<ListPreference>(PREF_PROFILE)!! }
    private val presetPref by lazy { findPreference<Preference>(PREF_PRESET)!! }
    private val ieqPref by lazy { findPreference<DolbyIeqPreference>(PREF_IEQ)!! }
    private val dialoguePref by lazy { findPreference<SwitchPreferenceCompat>(PREF_DIALOGUE)!! }
    private val dialogueAmountPref by lazy { findPreference<DolbySeekBarPreference>(PREF_DIALOGUE_AMOUNT)!! }
    private val bassPref by lazy { findPreference<SwitchPreferenceCompat>(PREF_BASS)!! }
    private val hpVirtPref by lazy { findPreference<SwitchPreferenceCompat>(PREF_HP_VIRTUALIZER)!! }
    private val spkVirtPref by lazy { findPreference<SwitchPreferenceCompat>(PREF_SPK_VIRTUALIZER)!! }
    private val settingsCategory by lazy { findPreference<PreferenceCategory>("dolby_category_settings")!! }
    private val advSettingsCategory by lazy { findPreference<PreferenceCategory>("dolby_category_adv_settings")!! }
    private val advSettingsFooter by lazy { findPreference<Preference>("dolby_adv_settings_footer")!! }
    private var volumePref: SwitchPreferenceCompat? = null
    private var stereoPref: DolbySeekBarPreference? = null

    private val dolbyController by lazy(LazyThreadSafetyMode.NONE) {
        DolbyController.getInstance(appContext)
    }
    private val audioManager by lazy(LazyThreadSafetyMode.NONE) {
        appContext.getSystemService(AudioManager::class.java)
    }
    private val handler = Handler(Looper.getMainLooper())

    private var isOnSpeaker = true
        set(value) {
            if (field == value) return
            field = value
            dlog(TAG, "setIsOnSpeaker($value)")
            updateProfileSpecificPrefs()
        }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
            dlog(TAG, "onAudioDevicesAdded")
            updateSpeakerState()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
            dlog(TAG, "onAudioDevicesRemoved")
            updateSpeakerState()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        dlog(TAG, "onCreatePreferences")
        setPreferencesFromResource(R.xml.dolby_settings, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Material 3 Expressive Design
        initializeMaterial3ExpressiveDesign()
        
        val profile = dolbyController.profile
        preferenceManager.preferenceDataStore = DolbyPreferenceStore(appContext).also {
            it.profile = profile
        }

        stereoPref = findPreference<DolbySeekBarPreference>(PREF_STEREO_WIDENING)!!
        if (!appContext.resources.getBoolean(R.bool.dolby_stereo_widening_supported)) {
            settingsCategory.removePreference(stereoPref!!)
            stereoPref = null
        }

        volumePref = findPreference<SwitchPreferenceCompat>(PREF_VOLUME)!!
        if (!appContext.resources.getBoolean(R.bool.dolby_volume_leveler_supported)) {
            advSettingsCategory.removePreference(volumePref!!)
            volumePref = null
        }

        val dsOn = dolbyController.dsOn
        switchBar.addOnSwitchChangeListener(this)
        switchBar.setChecked(dsOn)

        profilePref.onPreferenceChangeListener = this
        hpVirtPref.onPreferenceChangeListener = this
        spkVirtPref.onPreferenceChangeListener = this
        stereoPref?.apply {
            onPreferenceChangeListener = this@DolbySettingsFragment
            min = appContext.resources.getInteger(R.integer.stereo_widening_min)
            max = appContext.resources.getInteger(R.integer.stereo_widening_max)
        }
        dialoguePref.onPreferenceChangeListener = this
        dialogueAmountPref.apply {
            onPreferenceChangeListener = this@DolbySettingsFragment
            min = appContext.resources.getInteger(R.integer.dialogue_enhancer_min)
            max = appContext.resources.getInteger(R.integer.dialogue_enhancer_max)
        }
        bassPref.onPreferenceChangeListener = this
        volumePref?.onPreferenceChangeListener = this
        ieqPref.onPreferenceChangeListener = this

        audioManager.registerAudioDeviceCallback(audioDeviceCallback, handler)
        updateSpeakerState()
        updateProfileSpecificPrefsImmediate()
    }

    override fun onDestroyView() {
        dlog(TAG, "onDestroyView")
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        updateProfileSpecificPrefsImmediate()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        dlog(TAG, "onPreferenceChange: key=${preference.key} value=$newValue")
        when (preference.key) {
            PREF_PROFILE -> {
                val profile = newValue.toString().toInt()
                dolbyController.profile = profile
                updateProfileSpecificPrefs()
            }

            PREF_SPK_VIRTUALIZER -> {
                dolbyController.setSpeakerVirtEnabled(newValue as Boolean)
                // Material 3 switches handle UI updates automatically
            }

            PREF_HP_VIRTUALIZER -> {
                dolbyController.setHeadphoneVirtEnabled(newValue as Boolean)
                // Material 3 switches handle UI updates automatically
            }

            PREF_STEREO_WIDENING -> {
                dolbyController.setStereoWideningAmount(newValue as Int)
            }

            PREF_DIALOGUE -> {
                dolbyController.setDialogueEnhancerEnabled(newValue as Boolean)
                // Material 3 switches handle UI updates automatically
            }

            PREF_DIALOGUE_AMOUNT -> {
                dolbyController.setDialogueEnhancerAmount(newValue as Int)
            }

            PREF_BASS -> {
                dolbyController.setBassEnhancerEnabled(newValue as Boolean)
                // Material 3 switches handle UI updates automatically
            }

            PREF_VOLUME -> {
                dolbyController.setVolumeLevelerEnabled(newValue as Boolean)
                // Material 3 switches handle UI updates automatically
            }

            PREF_IEQ -> {
                dolbyController.setIeqPreset(newValue.toString().toInt())
            }

            else -> return false
        }
        return true
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        dlog(TAG, "onCheckedChanged($isChecked)")
        dolbyController.dsOn = isChecked
        updateProfileSpecificPrefs()
    }

    private fun updateSpeakerState() {
        val device = audioManager.getDevicesForAttributes(ATTRIBUTES_MEDIA)[0]
        isOnSpeaker = (device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
    }

    private fun updateProfileSpecificPrefs() {
        handler.postDelayed(100) { updateProfileSpecificPrefsImmediate() }
    }

    private fun updateProfileSpecificPrefsImmediate() {
        if (context == null) return
        if (!dolbyController.dsOn) {
            dlog(TAG, "updateProfileSpecificPrefs: Dolby is off")
            advSettingsCategory.isVisible = false
            return
        }

        val unknownRes = context?.getString(R.string.dolby_unknown) ?: "Unknown"
        val headphoneRes = context?.getString(R.string.dolby_connect_headphones) ?: "Connect headphones"
        val currentProfile = dolbyController.profile
        val isDynamicProfile = currentProfile == 0
        (preferenceManager.preferenceDataStore as DolbyPreferenceStore).profile = currentProfile

        dlog(TAG, "updateProfileSpecificPrefs: currentProfile=$currentProfile")

        profilePref.apply {
            if (entryValues.contains(currentProfile.toString())) {
                summary = "%s"
                value = currentProfile.toString()
            } else {
                summary = unknownRes
                dlog(TAG, "current profile $currentProfile unknown")
            }
        }

        // hide advanced settings on dynamic profile
        advSettingsCategory.isVisible = !isDynamicProfile
        advSettingsFooter.isVisible = isDynamicProfile

        presetPref.summary = dolbyController.getPresetName()
        
        // Force UI refresh for bass preference
        val bassEnabled = dolbyController.getBassEnhancerEnabled(currentProfile)
        dlog(TAG, "Setting bass preference: $bassEnabled")
        bassPref.isChecked = bassEnabled
        // Bass preference UI will be updated automatically

        // below prefs are not visible on dynamic profile
        if (isDynamicProfile) return

        val ieqValue = dolbyController.getIeqPreset(currentProfile)
        ieqPref.apply {
            if (entryValues.contains(ieqValue.toString())) {
                summary = "%s"
                value = ieqValue.toString()
            } else {
                summary = unknownRes
                dlog(TAG, "ieq value $ieqValue unknown")
            }
        }

        // Force UI refresh for all switch preferences
        val dialogueEnabled = dolbyController.getDialogueEnhancerEnabled(currentProfile)
        dlog(TAG, "Setting dialogue preference: $dialogueEnabled")
        dialoguePref.isChecked = dialogueEnabled
        // Dialogue preference UI will be updated automatically
        
        dialogueAmountPref.value = dolbyController.getDialogueEnhancerAmount(currentProfile)
        
        val spkVirtEnabled = dolbyController.getSpeakerVirtEnabled(currentProfile)
        dlog(TAG, "Setting speaker virtualizer preference: $spkVirtEnabled")
        spkVirtPref.isChecked = spkVirtEnabled
        // Speaker virtualizer preference UI will be updated automatically
        
        volumePref?.let { pref ->
            val volumeEnabled = dolbyController.getVolumeLevelerEnabled(currentProfile)
            dlog(TAG, "Setting volume leveler preference: $volumeEnabled")
            pref.isChecked = volumeEnabled
            // Volume preference UI will be updated automatically
        }
        
        val hpVirtEnabled = dolbyController.getHeadphoneVirtEnabled(currentProfile)
        dlog(TAG, "Setting headphone virtualizer preference: $hpVirtEnabled")
        hpVirtPref.isChecked = hpVirtEnabled
        // Headphone virtualizer preference UI will be updated automatically
        
        stereoPref?.value = dolbyController.getStereoWideningAmount(currentProfile)
    }

    /**
     * Initialize Material 3 Expressive Design features
     * Based on research from crDroid and other Android 16 implementations
     */
    private fun initializeMaterial3ExpressiveDesign() {
        try {
            // Check if Material 3 Expressive Design is enabled in config
            val isExpressiveEnabled = appContext.resources.getBoolean(
                appContext.resources.getIdentifier(
                    "is_expressive_design_enabled",
                    "bool",
                    appContext.packageName
                )
            )
            
            if (isExpressiveEnabled) {
                dlog(TAG, "Material 3 Expressive Design enabled")
                
                // Set system properties for Material 3 Expressive Design
                try {
                    SystemProperties.set("ro.config.is_expressive_design_enabled", "true")
                    SystemProperties.set("ro.config.dynamic_colors_enabled", "true")
                    SystemProperties.set("ro.config.material_you_enabled", "true")
                    SystemProperties.set("ro.config.expressive_animations_enabled", "true")
                    SystemProperties.set("ro.config.enhanced_ripples_enabled", "true")
                    SystemProperties.set("ro.config.expressive_typography_enabled", "true")
                    SystemProperties.set("ro.config.colorful_icon_backgrounds_enabled", "true")
                    
                    dlog(TAG, "Material 3 Expressive Design system properties set")
                } catch (e: Exception) {
                    dlog(TAG, "Failed to set system properties: ${e.message}")
                }
            } else {
                dlog(TAG, "Material 3 Expressive Design disabled in config")
            }
        } catch (e: Exception) {
            dlog(TAG, "Error initializing Material 3 Expressive Design: ${e.message}")
        }
    }

    /**
     * Refreshes the UI state after a reset operation to ensure all switches
     * and preferences reflect the current Dolby controller state
     */
    fun refreshUIAfterReset() {
        // Update the main Dolby switch state
        val dsOn = dolbyController.dsOn
        switchBar.setChecked(dsOn)
        
        // Update profile-specific preferences immediately
        updateProfileSpecificPrefsImmediate()
        
        // Force refresh all switch preferences to sync with controller state
        val currentProfile = dolbyController.profile
        (preferenceManager.preferenceDataStore as DolbyPreferenceStore).profile = currentProfile
        
        // Manually update each switch preference to ensure proper state synchronization
        bassPref.isChecked = dolbyController.getBassEnhancerEnabled()
        hpVirtPref.isChecked = dolbyController.getHeadphoneVirtEnabled()
        spkVirtPref.isChecked = dolbyController.getSpeakerVirtEnabled()
        dialoguePref.isChecked = dolbyController.getDialogueEnhancerEnabled()
        volumePref?.isChecked = dolbyController.getVolumeLevelerEnabled()
        
        // Update seek bar preferences
        stereoPref?.value = dolbyController.getStereoWideningAmount()
        dialogueAmountPref.value = dolbyController.getDialogueEnhancerAmount()
        
        // Update profile and IEQ preferences
        profilePref.value = currentProfile.toString()
        ieqPref.value = dolbyController.getIeqPreset().toString()
    }

    companion object {
        private const val TAG = "DolbySettingsFragment"
        private val ATTRIBUTES_MEDIA = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
    }
}
