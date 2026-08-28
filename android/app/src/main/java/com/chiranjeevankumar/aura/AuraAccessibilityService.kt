package com.chiranjeevankumar.aura

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * AURA system-level accessibility service.
 *
 * This service will allow AURA to perform Android-wide actions,
 * such as BACK and HOME, from outside the AURA Activity.
 */
class AuraAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // AURA does not need to process accessibility events yet.
    }

    override fun onInterrupt() {
        // Required by AccessibilityService.
    }

    /**
     * Performs the Android system Back action.
     *
     * Returns true when Android reports that the action
     * was successfully performed.
     */
    fun performBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }
}
