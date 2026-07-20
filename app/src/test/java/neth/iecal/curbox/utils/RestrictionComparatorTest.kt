package neth.iecal.curbox.utils

import neth.iecal.curbox.data.models.AppBlockerWarningScreenConfig
import neth.iecal.curbox.data.models.MindfulMessageConfig
import neth.iecal.curbox.data.models.ReelBlocker
import neth.iecal.curbox.data.models.SettingsChangeDelayConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Light off-device tests for [RestrictionComparator]. Each public clause returns true when the
 * proposed value is the same or stricter (applied immediately) and false when it weakens the
 * restriction (parked for the settings-change delay). These cover the everyday clauses; the NFC
 * unlock clause is covered separately in [RestrictionComparatorNfcTest].
 */
class RestrictionComparatorTest {

    private val warnBase = AppBlockerWarningScreenConfig()

    // --- changeDelay ---

    @Test
    fun `change delay that is off holds nothing so any change passes`() {
        val old = SettingsChangeDelayConfig(isEnabled = false)
        val next = SettingsChangeDelayConfig(isEnabled = true, delayMinutes = 5)
        assertTrue(RestrictionComparator.changeDelay(old, next))
    }

    @Test
    fun `raising the delay is same-or-stricter`() {
        val old = SettingsChangeDelayConfig(isEnabled = true, delayMinutes = 10)
        val next = old.copy(delayMinutes = 30)
        assertTrue(RestrictionComparator.changeDelay(old, next))
    }

    @Test
    fun `lowering the delay is a weakening`() {
        val old = SettingsChangeDelayConfig(isEnabled = true, delayMinutes = 30)
        val next = old.copy(delayMinutes = 10)
        assertFalse(RestrictionComparator.changeDelay(old, next))
    }

    @Test
    fun `turning off the tamper protection gate is a weakening`() {
        val old = SettingsChangeDelayConfig(isEnabled = true, delayMinutes = 10, requireTamperProtectionOff = true)
        val next = old.copy(requireTamperProtectionOff = false)
        assertFalse(RestrictionComparator.changeDelay(old, next))
    }

    // --- warningConfig cooldown & proceed clauses ---

    @Test
    fun `identical warning config is same-or-stricter`() {
        assertTrue(RestrictionComparator.warningConfig(warnBase, warnBase))
    }

    @Test
    fun `only changing wording is same-or-stricter`() {
        val next = warnBase.copy(message = "different", typingSentence = "type this")
        assertTrue(RestrictionComparator.warningConfig(warnBase, next))
    }

    @Test
    fun `a shorter cooldown window is stricter`() {
        val old = warnBase.copy(timeInterval = 60)
        val next = old.copy(timeInterval = 30)
        assertTrue(RestrictionComparator.warningConfig(old, next))
    }

    @Test
    fun `a longer cooldown window is a weakening`() {
        val old = warnBase.copy(timeInterval = 30)
        val next = old.copy(timeInterval = 60)
        assertFalse(RestrictionComparator.warningConfig(old, next))
    }

    @Test
    fun `re-enabling proceed after disabling it is a weakening`() {
        val old = warnBase.copy(isProceedDisabled = true)
        val next = old.copy(isProceedDisabled = false)
        assertFalse(RestrictionComparator.warningConfig(old, next))
    }

    @Test
    fun `raising the proceed delay is stricter`() {
        val old = warnBase.copy(proceedDelayInSecs = 5)
        val next = old.copy(proceedDelayInSecs = 10)
        assertTrue(RestrictionComparator.warningConfig(old, next))
    }

    @Test
    fun `allowing more proceeds is a weakening`() {
        val old = warnBase.copy(proceedLimitEnabled = true, allowedProceeds = 2, proceedsTimeWindowMn = 60)
        val next = old.copy(allowedProceeds = 5)
        assertFalse(RestrictionComparator.warningConfig(old, next))
    }

    // --- reelBlocker ---

    @Test
    fun `an inactive reel blocker imposes nothing so any change passes`() {
        val old = ReelBlocker(isActive = false)
        val next = ReelBlocker(isActive = true)
        assertTrue(RestrictionComparator.reelBlocker(old, next))
    }

    @Test
    fun `turning an active reel blocker off is a weakening`() {
        val old = ReelBlocker(isActive = true)
        val next = old.copy(isActive = false)
        assertFalse(RestrictionComparator.reelBlocker(old, next))
    }

    // --- mindfulMessages ---

    @Test
    fun `an inactive mindful message imposes nothing so any change passes`() {
        val old = MindfulMessageConfig(isActive = false)
        val next = MindfulMessageConfig(isActive = true, selectedApps = listOf("com.example"))
        assertTrue(RestrictionComparator.mindfulMessages(old, next))
    }

    @Test
    fun `dropping a covered app from an active mindful message is a weakening`() {
        val old = MindfulMessageConfig(isActive = true, selectedApps = listOf("a", "b"))
        val next = old.copy(selectedApps = listOf("a"))
        assertFalse(RestrictionComparator.mindfulMessages(old, next))
    }
}
