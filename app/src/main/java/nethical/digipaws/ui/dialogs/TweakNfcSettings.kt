package nethical.digipaws.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nethical.digipaws.Constants
import nethical.digipaws.R
import nethical.digipaws.databinding.DialogNfcSettingsBinding
import nethical.digipaws.utils.SavedPreferencesLoader

class TweakNfcSettings(
    savedPreferencesLoader: SavedPreferencesLoader
) : BaseDialog(savedPreferencesLoader) {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogNfcSettingsBinding.inflate(layoutInflater)

        val currentSettings = savedPreferencesLoader?.getNfcFocusSettings()
            ?: SavedPreferencesLoader.NfcFocusSettings()

        binding.nfcEnabledSwitch.isChecked = currentSettings.isEnabled
        updateControlsEnabled(binding, currentSettings.isEnabled)

        binding.nfcEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateControlsEnabled(binding, isChecked)
        }

        binding.nfcDurationPicker.minValue = 1
        binding.nfcDurationPicker.maxValue = 180
        binding.nfcDurationPicker.setValue(currentSettings.defaultDurationMins)
        binding.nfcDurationPicker.setUnit("mins")

        when (currentSettings.modeType) {
            Constants.FOCUS_MODE_BLOCK_ALL_EX_SELECTED -> binding.nfcBlockAll.isChecked = true
            Constants.FOCUS_MODE_BLOCK_SELECTED -> binding.nfcBlockSelected.isChecked = true
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.save)) { dialog, _ ->
                val selectedMode = when (binding.nfcModeType.checkedRadioButtonId) {
                    binding.nfcBlockAll.id -> Constants.FOCUS_MODE_BLOCK_ALL_EX_SELECTED
                    binding.nfcBlockSelected.id -> Constants.FOCUS_MODE_BLOCK_SELECTED
                    else -> Constants.FOCUS_MODE_BLOCK_ALL_EX_SELECTED
                }

                val newSettings = SavedPreferencesLoader.NfcFocusSettings(
                    isEnabled = binding.nfcEnabledSwitch.isChecked,
                    defaultDurationMins = binding.nfcDurationPicker.getValue(),
                    modeType = selectedMode
                )
                savedPreferencesLoader?.saveNfcFocusSettings(newSettings)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

    private fun updateControlsEnabled(binding: DialogNfcSettingsBinding, enabled: Boolean) {
        binding.nfcDurationLabel.isEnabled = enabled
        binding.nfcDurationPicker.isEnabled = enabled
        binding.nfcModeType.isEnabled = enabled
        binding.nfcBlockAll.isEnabled = enabled
        binding.nfcBlockSelected.isEnabled = enabled

        val alpha = if (enabled) 1.0f else 0.5f
        binding.nfcDurationLabel.alpha = alpha
        binding.nfcDurationPicker.alpha = alpha
        binding.nfcModeType.alpha = alpha
    }
}
