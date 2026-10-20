package com.universal.remote.ads

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Wraps Google's User Messaging Platform (UMP) SDK: requests consent info, shows the
 * consent/GDPR (General Data Protection Regulation) form only when required, and never blocks the
 * app flow on a UMP error.
 */
object ConsentManager {

    private const val TAG = "ConsentManager"

    /** Set to a real device's advertising-id-derived hash to force-test the EEA consent form. */
    private val TEST_DEVICE_IDS: List<String> = emptyList()

    /**
     * Whether a debug build pretends to be inside the EEA (European Economic Area) so the GDPR
     * (General Data Protection Regulation) form is demanded on every launch. **Off, deliberately.**
     *
     * UMP (User Messaging Platform) resolves which privacy message to show from the AdMob
     * application id in the manifest, and ours is still Google's sample id, which has no message
     * published under this account. An emulator is always a UMP test device, so the forced
     * geography does take effect - and then the form cannot resolve, `loadAndShowConsentFormIfRequired`
     * sits until it gives up, and [gatherConsent] answers `canRequestAds = false`. Measured: a
     * 35-second stall on every launch, after which every loader correctly declines to request, so
     * no ad of any kind appears anywhere in the app. That is not the EEA path being tested; it is
     * the EEA path being unreachable.
     *
     * **The EEA path is still untested, and testing it is a release gate.** Once a real AdMob
     * application id is in the manifest and a privacy message is published for it, flip this to true
     * and verify the form appears, that consent and refusal both persist, and that `canRequestAds`
     * follows the answer. Shipping without having done that means shipping a consent flow nobody
     * has seen work.
     */
    private const val FORCE_EEA_CONSENT_FORM = false

    /**
     * Requests/updates consent info and shows the consent form if required. [onComplete] is
     * always invoked exactly once, whether or not a form was shown, so callers can proceed with
     * the rest of the ad flow unconditionally.
     */
    fun gatherConsent(activity: Activity, onComplete: (canRequestAds: Boolean) -> Unit) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        val paramsBuilder = ConsentRequestParameters.Builder()
        // Debug settings at all only when there is something to say: a forced geography, or test
        // devices to name. With neither, attaching an empty ConsentDebugSettings would only tell UMP
        // to treat this build as a test device for no gain - see [FORCE_EEA_CONSENT_FORM].
        if (AdsController.isDebug && (FORCE_EEA_CONSENT_FORM || TEST_DEVICE_IDS.isNotEmpty())) {
            val debugSettings = ConsentDebugSettings.Builder(activity)
                .apply {
                    TEST_DEVICE_IDS.forEach { addTestDeviceHashedId(it) }
                    if (FORCE_EEA_CONSENT_FORM) {
                        setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    }
                }
                .build()
            paramsBuilder.setConsentDebugSettings(debugSettings)
        }
        val params = paramsBuilder.build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.message}")
                    }
                    onComplete(consentInformation.canRequestAds())
                }
            },
            { requestError ->
                Log.w(TAG, "Consent info update failed: ${requestError.message}")
                onComplete(consentInformation.canRequestAds())
            }
        )
    }
}
