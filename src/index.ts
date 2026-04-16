import { requireNativeModule, EventEmitter, EventSubscription } from 'expo-modules-core';
import { SolanaNfcEvents } from './SolanaP2Pnfc.types';

const SolanaP2Pnfc = requireNativeModule('SolanaP2Pnfc');
const emitter = new EventEmitter<SolanaNfcEvents>(SolanaP2Pnfc as any);

/**
 * Starts broadcasting the provided URI via NFC (Host Card Emulation).
 * Works with any URI scheme, including custom ones like "solana:".
 *
 * @param url The URI to share (e.g. "solana:Fw35M...?amount=0.01").
 */
export function startSharing(url: string): void {
  return SolanaP2Pnfc.startSharing(url);
}

/**
 * Stops the NFC broadcast and disables the background service.
 *
 * CRITICAL: Always call this when your screen unmounts to prevent battery
 * drain and interference with the user's default NFC apps (e.g. Google Wallet).
 */
export function stopSharing(): void {
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
 *   const sub = addNfcReadListener(() => console.log('Read!'));
 *   return () => sub.remove();
 * }, []);
 */
export function addNfcReadListener(listener: () => void): EventSubscription {
  return emitter.addListener('onNfcRead', listener);
}