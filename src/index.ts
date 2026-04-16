import { requireNativeModule, EventEmitter, EventSubscription } from 'expo-modules-core';
import { Platform } from 'react-native';
import { SolanaNfcEvents } from './SolanaP2Pnfc.types';

const SolanaP2Pnfc = Platform.OS === 'android' ? requireNativeModule('SolanaP2Pnfc') : null;

const emitter = SolanaP2Pnfc ? new EventEmitter<SolanaNfcEvents>(SolanaP2Pnfc as any) : null;

/**
 * Starts broadcasting the provided URI via NFC (Host Card Emulation).
 * Works with any URI scheme, including custom ones like "solana:".
 *
 * @param url The URI to share (e.g. "solana:Fw35M...?amount=0.01").
 */
export function startSharing(url: string): void {
  if (Platform.OS !== 'android' || !SolanaP2Pnfc) {
    console.warn('[@nextvibe/solana-p2p-nfc] startSharing is only supported on Android. iOS devices can only act as receivers.');
    return;
  }
  return SolanaP2Pnfc.startSharing(url);
}

/**
 * Stops the NFC broadcast and disables the background service.
 *
 * CRITICAL: Always call this when your screen unmounts to prevent battery
 * drain and interference with the user's default NFC apps (e.g. Google Wallet).
 */
export function stopSharing(): void {
  if (Platform.OS !== 'android' || !SolanaP2Pnfc) return;
  return SolanaP2Pnfc.stopSharing();
}

/**
 * Subscribes to the event emitted when another device successfully reads the tag.
 *
 * @param listener Callback executed on a successful read.
 * @returns EventSubscription — call `.remove()` in your cleanup hook.
 *
 * @example
 * useEffect(() => {
 * const sub = addNfcReadListener(() => console.log('Read!'));
 * return () => sub.remove();
 * }, []);
 */
export function addNfcReadListener(listener: () => void): EventSubscription {
  if (!emitter) {
    return { remove: () => {} } as EventSubscription;
  }
  return emitter.addListener('onNfcRead', listener);
}