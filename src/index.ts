// Reexport the native module. On web, it will be resolved to SolanaP2PnfcModule.web.ts
// and on native platforms to SolanaP2PnfcModule.ts
export { default } from './SolanaP2PnfcModule';
export { default as SolanaP2PnfcView } from './SolanaP2PnfcView';
export * from  './SolanaP2Pnfc.types';
