import { requireNativeModule, EventEmitter, EventSubscription } from 'expo-modules-core';
import { SolanaNfcEvents } from './SolanaP2Pnfc.types';

const SolanaP2Pnfc = requireNativeModule('SolanaP2Pnfc');

const emitter = new EventEmitter<SolanaNfcEvents>(SolanaP2Pnfc as any);

/**
 * Starts broadcasting the provided URL via NFC (Host Card Emulation).
 * This dynamically turns the Android device into an NFC tag.
 * * @param url The link or data to be shared (e.g., "https://nextvibe.io").
 */
export function startSharing(url: string): void {
  return SolanaP2Pnfc.startSharing(url);
}

/**
 * Stops the NFC broadcast and disables the background service.
 * CRITICAL: Always call this when your component unmounts to prevent battery drain
 * and interference with the user's default NFC apps (like Google Wallet).
 */
export function stopSharing(): void {
  return SolanaP2Pnfc.stopSharing();
}

/**
 * Subscribes to the event emitted when another device successfully reads the NFC tag.
 * * @param listener Callback function executed on successful read.
 * @returns An EventSubscription object. You MUST call `.remove()` on this in your cleanup hook.
 * * @example
 * useEffect(() => {
 * const sub = addNfcReadListener(() => console.log('Successfully read!'));
 * return () => sub.remove(); // Cleanup is mandatory!
 * }, []);
 */
export function addNfcReadListener(listener: () => void): EventSubscription {
  return emitter.addListener('onNfcRead', listener);
}