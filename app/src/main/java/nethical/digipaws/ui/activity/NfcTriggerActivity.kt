package nethical.digipaws.ui.activity

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import nethical.digipaws.R
import nethical.digipaws.blockers.FocusModeBlocker
import nethical.digipaws.services.AppBlockerService
import nethical.digipaws.utils.NotificationTimerManager
import nethical.digipaws.utils.SavedPreferencesLoader

class NfcTriggerActivity : AppCompatActivity() {

    private val savedPreferencesLoader = SavedPreferencesLoader(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED != intent.action) {
            finish()
            return
        }

        val physicalTag = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, android.nfc.Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
        if (physicalTag == null) {
            finish()
            return
        }

        val nfcSettings = savedPreferencesLoader.getNfcFocusSettings()
        if (!nfcSettings.isEnabled) {
            Toast.makeText(this, R.string.nfc_trigger_disabled, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val uri = parseNdefUri(intent)
        if (uri == null) {
            Toast.makeText(this, R.string.nfc_invalid_payload, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val pathSegments = uri.removePrefix("digipaws://focus").split("/").filter { it.isNotEmpty() }
        val action = pathSegments.getOrNull(0)
        val durationFromUri = pathSegments.getOrNull(1)?.toIntOrNull()

        when (action) {
            "toggle" -> toggleFocusMode(durationFromUri)
            "start" -> startFocusMode(durationFromUri)
            "stop" -> stopFocusMode()
            else -> {
                Toast.makeText(this, R.string.nfc_invalid_payload, Toast.LENGTH_SHORT).show()
            }
        }

        finish()
    }

    private fun parseNdefUri(intent: Intent): String? {
        val rawMessages = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        }
        if (rawMessages.isNullOrEmpty()) return null

        val ndefMessage = rawMessages[0] as? NdefMessage ?: return null
        val record = ndefMessage.records.firstOrNull() ?: return null

        return try {
            record.toUri()?.toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun toggleFocusMode(durationFromUri: Int?) {
        val focusModeData = savedPreferencesLoader.getFocusModeData()
        if (focusModeData.isTurnedOn) {
            stopFocusMode()
        } else {
            startFocusMode(durationFromUri)
        }
    }

    private fun startFocusMode(durationFromUri: Int?) {
        val currentData = savedPreferencesLoader.getFocusModeData()
        if (currentData.isTurnedOn) {
            Toast.makeText(this, R.string.nfc_focus_mode_already_active, Toast.LENGTH_SHORT).show()
            return
        }

        val nfcSettings = savedPreferencesLoader.getNfcFocusSettings()

        val durationMins = durationFromUri ?: nfcSettings.defaultDurationMins
        val durationMillis = durationMins * 60 * 1000L

        val newFocusModeData = FocusModeBlocker.FocusModeData(
            isTurnedOn = true,
            endTime = System.currentTimeMillis() + durationMillis,
            modeType = nfcSettings.modeType,
            selectedApps = currentData.selectedApps
        )

        savedPreferencesLoader.saveFocusModeData(newFocusModeData)
        sendBroadcast(Intent(AppBlockerService.INTENT_ACTION_REFRESH_FOCUS_MODE))

        val timer = NotificationTimerManager(this)
        timer.startTimer(durationMillis)

        Toast.makeText(this, getString(R.string.nfc_focus_mode_started_mins, durationMins), Toast.LENGTH_SHORT).show()
    }

    private fun stopFocusMode() {
        val currentData = savedPreferencesLoader.getFocusModeData()
        if (!currentData.isTurnedOn) {
            Toast.makeText(this, R.string.nfc_focus_mode_not_active, Toast.LENGTH_SHORT).show()
            return
        }

        val newFocusModeData = FocusModeBlocker.FocusModeData(
            isTurnedOn = false,
            endTime = -1,
            modeType = currentData.modeType,
            selectedApps = currentData.selectedApps
        )

        savedPreferencesLoader.saveFocusModeData(newFocusModeData)
        sendBroadcast(Intent(AppBlockerService.INTENT_ACTION_REFRESH_FOCUS_MODE))

        val timer = NotificationTimerManager(this)
        timer.stopTimer()

        Toast.makeText(this, R.string.nfc_focus_mode_stopped, Toast.LENGTH_SHORT).show()
    }
}
