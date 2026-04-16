package com.nextvibe.solanap2pnfc

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

/**
 * Expo Module bridge — exposes native Android NFC HCE capabilities to React Native.
 */
class SolanaP2PnfcModule : Module() {

    override fun definition() = ModuleDefinition {

        Name("SolanaP2Pnfc")

        Events("onNfcRead")

        /**
         * Starts NFC Host Card Emulation to broadcast [url].
         */
        Function("startSharing") { url: String ->
            // 1. Update the URL the HCE service will serve
            NdefHostApduService.urlToShare = url

            // 2. Register the callback that fires when a reader completes the transaction.
            //    The HCE service runs on a background system thread, so we hop to Main
            //    before touching the React Native event emitter.
            NdefHostApduService.onReadListener = {
                Handler(Looper.getMainLooper()).post {
                    try {
                        sendEvent("onNfcRead")
                    } catch (_: Exception) {
                        // JS context may already be gone — safe to ignore
                    }
                }
            }

            // 3. Dynamically enable the HCE service component
            setServiceEnabled(true)
        }

        /**
         * Stops NFC broadcasting and releases system resources.
         */
        Function("stopSharing") {
            NdefHostApduService.onReadListener = null
            setServiceEnabled(false)
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun setServiceEnabled(enabled: Boolean) {
        val context = appContext.reactContext ?: return
        val newState = if (enabled)
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED

        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, NdefHostApduService::class.java),
            newState,
            PackageManager.DONT_KILL_APP
        )
    }
}