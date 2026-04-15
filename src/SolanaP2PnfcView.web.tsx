import * as React from 'react';

import { SolanaP2PnfcViewProps } from './SolanaP2Pnfc.types';

export default function SolanaP2PnfcView(props: SolanaP2PnfcViewProps) {
  return (
    <div>
      <iframe
        style={{ flex: 1 }}
        src={props.url}
        onLoad={() => props.onLoad({ nativeEvent: { url: props.url } })}
      />
    </div>
  );
}
