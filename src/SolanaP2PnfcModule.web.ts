import { registerWebModule, NativeModule } from 'expo';

import { SolanaP2PnfcModuleEvents } from './SolanaP2Pnfc.types';

class SolanaP2PnfcModule extends NativeModule<SolanaP2PnfcModuleEvents> {
  PI = Math.PI;
  async setValueAsync(value: string): Promise<void> {
    this.emit('onChange', { value });
  }
  hello() {
    return 'Hello world! 👋';
  }
}

export default registerWebModule(SolanaP2PnfcModule, 'SolanaP2PnfcModule');
