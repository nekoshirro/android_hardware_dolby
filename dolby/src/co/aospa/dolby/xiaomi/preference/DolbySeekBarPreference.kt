/*
 * Copyright (C) 2023-25 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.preference

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference
import co.aospa.dolby.xiaomi.R

/**
 * Custom SeekBarPreference that displays numeric value indicator
 * for Dolby strength controls
 */
class DolbySeekBarPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : SeekBarPreference(context, attrs, defStyleAttr, defStyleRes) {

    private var valueTextView: TextView? = null
    private var seekBar: SeekBar? = null

    init {
        layoutResource = R.layout.preference_seekbar_material3_expressive
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        
        // Find the value TextView and SeekBar
        valueTextView = holder.findViewById(R.id.seekbar_value) as? TextView
        seekBar = holder.findViewById(R.id.seekbar) as? SeekBar
        
        // Update the value display
        updateValueDisplay()
        
        // Set up SeekBar listener to update value in real-time
        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val actualValue = progress + min
                    updateValueDisplay(actualValue)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val actualValue = (seekBar?.progress ?: 0) + min
                value = actualValue
                callChangeListener(actualValue)
            }
        })
    }
    
    override fun setValue(value: Int) {
        super.setValue(value)
        updateValueDisplay(value)
    }
    
    private fun updateValueDisplay(displayValue: Int = value) {
        valueTextView?.let { textView ->
            // Format the value with better context
            val formattedValue = when {
                max <= 10 -> {
                    // For small ranges (0-10), show as simple numbers
                    displayValue.toString()
                }
                max <= 100 -> {
                    // For medium ranges, show with percentage context
                    val percentage = ((displayValue - min).toFloat() / (max - min).toFloat() * 100).toInt()
                    "$displayValue ($percentage%)"
                }
                else -> {
                    // For large ranges, show with unit context
                    "$displayValue"
                }
            }
            textView.text = formattedValue
            
            // Add subtle animation for value changes
            textView.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(100)
                .withEndAction {
                    textView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start()
                }
                .start()
        }
    }
}