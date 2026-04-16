import React, { useState, useEffect, useCallback } from 'react';
import {
  View, Text, TextInput, TouchableOpacity,
  StyleSheet, SafeAreaView, StatusBar, Platform,
} from 'react-native';
import { startSharing, stopSharing, addNfcReadListener } from '@nextvibe/solana-p2p-nfc';

const DEFAULT_URL =
  'solana:Fw35M3Pmb1YhBw6F85xQn1N3V63T16uXX19U3d4z5jD9' +
  '?amount=0.01&label=NextVibe&message=IRL%20Connect';

export default function App() {
  const [url, setUrl]           = useState(DEFAULT_URL);
  const [isSharing, setSharing] = useState(false);
  const [readCount, setCount]   = useState(0);

  useEffect(() => {
    const sub = addNfcReadListener(() => {
      setCount(prev => prev + 1);
    });
    return () => {
      sub.remove();
      stopSharing();
    };
  }, []);

  const handleStart = useCallback(() => {
    if (!url.trim()) return;
    startSharing(url.trim());
    setSharing(true);
  }, [url]);

  const handleStop = useCallback(() => {
    stopSharing();
    setSharing(false);
  }, []);

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar barStyle="dark-content" backgroundColor="#f2f2f7"/>

      <View style={styles.container}>
        <View style={styles.card}>

          {/* Header */}
          <Text style={styles.header}>⬡ Solana Pay NFC</Text>

          {/* URL input */}
          <Text style={styles.label}>Solana Pay URL</Text>
          <TextInput
            style={[styles.input, isSharing && styles.inputDisabled]}
            value={url}
            onChangeText={setUrl}
            editable={!isSharing}
            placeholder="solana:..."
            placeholderTextColor="#aaa"
            autoCapitalize="none"
            autoCorrect={false}
            multiline
          />

          {/* Buttons */}
          <View style={styles.row}>
            <TouchableOpacity
              style={[styles.btn, styles.btnStart, isSharing && styles.btnDisabled]}
              onPress={handleStart}
              disabled={isSharing}
              activeOpacity={0.8}
            >
              <Text style={styles.btnText}>▶  Start</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[styles.btn, styles.btnStop, !isSharing && styles.btnDisabled]}
              onPress={handleStop}
              disabled={!isSharing}
              activeOpacity={0.8}
            >
              <Text style={styles.btnText}>■  Stop</Text>
            </TouchableOpacity>
          </View>

          {/* Status */}
          <View style={styles.status}>
            <View style={[styles.dot, isSharing ? styles.dotOn : styles.dotOff]}/>
            <Text style={styles.statusText}>
              {isSharing ? 'Broadcasting via NFC…' : 'Idle'}
            </Text>
          </View>

          {/* Counter */}
          {readCount > 0 && (
            <Text style={styles.counter}>
              ✅ {readCount} successful read{readCount !== 1 ? 's' : ''}
            </Text>
          )}

        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: '#f2f2f7' },
  container: {
    flex: 1,
    justifyContent: 'center',
    padding: 20,
  },
  card: {
    backgroundColor: '#fff',
    borderRadius: 20,
    padding: 24,
    // iOS shadow
    shadowColor: '#000',
    shadowOpacity: 0.08,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 4 },
    // Android shadow
    elevation: 6,
  },
  header: {
    fontSize: 22,
    fontWeight: '700',
    textAlign: 'center',
    marginBottom: 24,
    color: '#1c1c1e',
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
    color: '#8e8e93',
    marginBottom: 6,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  input: {
    borderWidth: 1,
    borderColor: '#d1d1d6',
    borderRadius: 10,
    padding: 12,
    fontSize: 13,
    color: '#1c1c1e',
    backgroundColor: '#fafafa',
    minHeight: 72,
    marginBottom: 20,
    fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
  },
  inputDisabled: { opacity: 0.5 },
  row: {
    flexDirection: 'row',
    gap: 12,
    marginBottom: 24,
  },
  btn: {
    flex: 1,
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: 'center',
  },
  btnStart: { backgroundColor: '#007AFF' },
  btnStop:  { backgroundColor: '#FF3B30' },
  btnDisabled: { opacity: 0.35 },
  btnText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  status: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  dot: {
    width: 10,
    height: 10,
    borderRadius: 5,
  },
  dotOn:  { backgroundColor: '#34C759' },
  dotOff: { backgroundColor: '#C7C7CC' },
  statusText: {
    fontSize: 15,
    color: '#3c3c43',
    fontWeight: '500',
  },
  counter: {
    marginTop: 16,
    textAlign: 'center',
    fontSize: 14,
    color: '#34C759',
    fontWeight: '600',
  },
});