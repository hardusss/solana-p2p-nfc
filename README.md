# ⚡ NextVibe Solana P2P NFC

[![npm version](https://img.shields.io/npm/v/@nextvibe/solana-p2p-nfc.svg?style=flat)](https://www.npmjs.com/package/@nextvibe/solana-p2p-nfc)
[![platform](https://img.shields.io/badge/platform-Android%20%7C%20iOS-blue.svg)](#)

A production-ready Expo Native Module that enables true peer-to-peer (phone-to-phone) NFC communication using APDU commands. Built specifically to solve the "tap-to-pay" UX for Solana Pay IRL transactions.

*Created for the Superteam Ukraine Bounty.*

---

## 🎯 The Problem

Solana Pay is powerful, but in-person transactions currently rely on static QR codes or pre-programmed physical NFC tags. Phone-to-phone NFC is the obvious alternative, but it breaks in practice:
* **Bluetooth-based P2P** destroys the UX with pairing flows and friction.
* **Raw NFC deeplinks on iOS** trigger Apple Wallet / Apple Pay instead of the intended crypto wallet, completely breaking the transaction flow.

## ✨ Our Solution & The "iOS Wallet Bypass" Magic

Because Apple restricts Host Card Emulation (HCE) on iOS, this module operates in an **asymmetric P2P architecture**: Android devices act as dynamic NFC emitters, while both iOS and Android devices can act as receivers.

**How we bypass Apple Wallet on iOS:**
Standard NFC libraries typically encode URIs using specific identifier codes (e.g., `0x01` for `http://www.`, `0x02` for `https://www.`). When an iPhone reads these or standard payment AIDs, the OS aggressively intercepts them, assuming it's a legacy payment terminal, and pops up the Apple Pay UI.

We solved this by crafting a custom NDEF payload at the APDU level. By using the `0x00` (No Prefix) identifier code and manually injecting custom URI schemes (like `solana:`), we force iOS to treat the payload as a raw Universal Link / Deep Link. This bypasses the Apple Wallet daemon entirely and routes the payload directly to the target app (e.g., Phantom Wallet).

## 🚀 Features

* **Dynamic Payload Generation:** Transmit URLs created on the fly (no static tags required).
* **iOS Wallet Bypass:** Send data to iPhones without triggering Apple Pay.
* **Universal Link Support:** Send `solana:`, `https:`, or any arbitrary URI.
* **Automatic Configuration:** Zero manual linking. Expo Config Plugins handle all Android Manifests automatically.

## 📦 Installation

```bash
npm install @nextvibe/solana-p2p-nfc
```

## ⚙️ Configuration (Under the Hood)

This package is built using the modern Expo Modules API. **You do not need to manually edit any native files.**

During the build process, the Expo Config Plugin automatically merges the following into your Android project:
1. **`AndroidManifest.xml`:** Injects the `BIND_NFC_SERVICE` permission and registers the `NdefHostApduService`. It sets `android:enabled="false"` by default to prevent battery drain when the app is idle.
2. **`apduservice.xml`:** Maps the standard NFC Forum Type 4 Tag AID (`D2760000850101`) to the service, allowing the OS to route incoming NFC taps to your app.

## 💻 Usage

Here is a minimal example of how to implement the P2P tap flow in your React Native / Expo app:

```tsx
import React, { useState, useEffect } from 'react';
import { View, Button, Text } from 'react-native';
import { startSharing, stopSharing, addNfcReadListener } from '@nextvibe/solana-p2p-nfc';

export default function SolanaPayTap() {
  const [isSharing, setIsSharing] = useState(false);

  // Your dynamic Solana Pay URL
  const url = 'solana:Fw35M3Pmb1YhBw6F85xQn1N3V63T16uXX19U3d4z5jD9?amount=0.01&label=NextVibe';

  useEffect(() => {
    // 1. Listen for successful physical taps
    const subscription = addNfcReadListener(() => {
      console.log('NFC tag was successfully read by another device!');
    });

    // 2. CRITICAL: Always clean up to prevent battery drain
    return () => {
      subscription.remove();
      stopSharing();
    };
  }, []);

  const handleStart = () => {
    startSharing(url);
    setIsSharing(true);
  };

  const handleStop = () => {
    stopSharing();
    setIsSharing(false);
  };

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <Text>Status: {isSharing ? '📡 Broadcasting...' : '🛑 Idle'}</Text>
      
      <Button title="Start Tap-to-Pay" onPress={handleStart} disabled={isSharing} />
      <Button title="Stop" onPress={handleStop} disabled={!isSharing} color="red" />
    </View>
  );
}
```

## 📖 API Reference

### `startSharing(url: string): void`
Dynamically enables the HCE background service and begins broadcasting the provided URI.

### `stopSharing(): void`
Disables the HCE service. **Must** be called when the component unmounts or the transaction is complete to release system resources and avoid conflicting with system wallets.

### `addNfcReadListener(listener: () => void): EventSubscription`
Subscribes to the event emitted when a receiving device successfully reads the entire NDEF payload via APDU commands. Returns a subscription object that must be removed via `.remove()`.

## 🛠️ Troubleshooting & FAQ

**Q: I called `startSharing()` but the other phone doesn't react.**
**A:** Ensure the Android device acting as the emitter has NFC turned on and the screen is unlocked. On the receiving iOS device, ensure the screen is on (iOS does not read NFC while locked).

**Q: When I tap, nothing happens, or the OS opens a different app instead of mine.**
**A:** **AID Conflict.** Android routes NFC requests based on the Application ID (AID). This module uses the standard `D2760000850101`. If you have other development apps installed on your emitter phone that also use this AID, the OS might route the request to them instead. *Solution: Uninstall older testing apps/builds and try again.*

**Q: Can I use this with Expo Go?**
**A:** No. Since this module includes custom native Kotlin code and Android Manifest modifications, it requires a Development Build (`npx expo run:android` or EAS Build).

---

### License
MIT