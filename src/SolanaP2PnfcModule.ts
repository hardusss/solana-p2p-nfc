import { NativeModule, requireNativeModule } from 'expo';

import { SolanaP2PnfcModuleEvents } from './SolanaP2Pnfc.types';

declare class SolanaP2PnfcModule extends NativeModule<SolanaP2PnfcModuleEvents> {
  PI: number;
  hello(): string;
  setValueAsync(value: string): Promise<void>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<SolanaP2PnfcModule>('SolanaP2Pnfc');
