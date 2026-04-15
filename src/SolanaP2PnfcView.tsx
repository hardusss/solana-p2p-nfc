import { requireNativeView } from 'expo';
import * as React from 'react';

import { SolanaP2PnfcViewProps } from './SolanaP2Pnfc.types';

const NativeView: React.ComponentType<SolanaP2PnfcViewProps> =
  requireNativeView('SolanaP2Pnfc');

export default function SolanaP2PnfcView(props: SolanaP2PnfcViewProps) {
  return <NativeView {...props} />;
}
