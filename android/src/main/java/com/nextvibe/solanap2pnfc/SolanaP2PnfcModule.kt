package com.nextvibe.solanap2pnfc

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

/**
 * The Expo Module bridge that exposes native Android NFC HCE capabilities to React Native (JS/TS).
 */
class SolanaP2PnfcModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("SolanaP2Pnfc") 

    // Defines the events that this native module can emit to the JavaScript side
    Events("onNfcRead")

    /**
     * Starts the NFC Host Card Emulation (HCE) to broadcast the provided URL.
     * @param url The string (e.g., Solana transaction link) to be shared via NFC.
     */
    Function("startSharing") { url: String ->
      // 1. Pass the data to the background NFC service state
      NdefHostApduService.urlToShare = url
      
      // 2. Set up the callback for when another device successfully reads the NFC payload
      NdefHostApduService.onReadListener = {
          // The NFC transaction occurs on a background system thread.
          // We must use a Handler to hop back to the Main (UI) thread before sending
          // the event to React Native. Doing this on a background thread can cause crashes.
          Handler(Looper.getMainLooper()).post {
              try {
                  this@SolanaP2PnfcModule.sendEvent("onNfcRead")
              } catch (e: Exception) {
                  // Silently catch exceptions in case the JS context is already destroyed
              }
          }
      }

      // 3. Dynamically enable the HCE Service
      val context = appContext.reactContext
      if (context != null) {
          val pm = context.packageManager
          pm.setComponentEnabledSetting(
              ComponentName(context, NdefHostApduService::class.java),
              PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
              PackageManager.DONT_KILL_APP
          )
      }
    }

    /**
     * Stops the NFC broadcasting and releases system resources.
     */
    Function("stopSharing") {
      // Clear the listener to prevent memory leaks and rogue callbacks
      NdefHostApduService.onReadListener = null 
      
      // Dynamically disable the HCE Service.
      val context = appContext.reactContext
      if (context != null) {
          val pm = context.packageManager
          pm.setComponentEnabledSetting(
              ComponentName(context, NdefHostApduService::class.java),
              PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
              PackageManager.DONT_KILL_APP
          )
      }
    }
  }
}