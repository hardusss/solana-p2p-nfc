import React, { useState, useEffect } from 'react';
import { Button, SafeAreaView, Text, View, TextInput, StyleSheet } from 'react-native';

// Import our custom bridge functions
import { startSharing, stopSharing, addNfcReadListener } from '@nextvibe/solana-p2p-nfc';

export default function App() {
  const [isSharing, setIsSharing] = useState(false);
  const [readCount, setReadCount] = useState(0);
  
  // Hardcoded Solana Pay transaction link.
  // Format: solana:<recipient_address>?amount=<amount>&label=<merchant_name>&message=<memo>
  const [url, setUrl] = useState(
    'solana:DanyLoxxxXXXXxxxxXXXXxxxxXXXXxxxxXXXXxxxxNV?amount=0.1&label=NextVibe&message=P2P%20Transfer'
  );

  useEffect(() => {
    // Subscribe to the event emitted when the NFC tag is successfully read by another device
    const subscription = addNfcReadListener(() => {
      console.log('NFC tag was successfully read!');
      setReadCount((prev) => prev + 1);
    });

    // Cleanup function: executed when the component unmounts
    return () => {
      // CRITICAL: Remove the listener to prevent memory leaks
      subscription.remove(); 
      // CRITICAL: Stop the background NFC service to prevent battery drain 
      // and interference with the OS (e.g., Google Wallet)
      stopSharing();         
    };
  }, []);

  const handleStart = () => {
    if (!url) return;
    
    // Start the Host Card Emulation (HCE) with the provided URL
    startSharing(url);
    setIsSharing(true);
  };

  const handleStop = () => {
    // Disable the HCE background service
    stopSharing();
    setIsSharing(false);
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.card}>
        <Text style={styles.header}>Solana Pay NFC Sender</Text>

        <Text style={styles.label}>Solana Pay URL (NDEF Payload):</Text>
        <TextInput
          style={styles.input}
          value={url}
          onChangeText={setUrl}
          editable={!isSharing}
          placeholder="solana:..."
          autoCapitalize="none"
          multiline={true}
        />

        <View style={styles.buttonRow}>
          <Button 
            title="Start Sharing" 
            onPress={handleStart} 
            disabled={isSharing} 
          />
          <View style={{ width: 10 }} />
          <Button 
            title="Stop Sharing" 
            onPress={handleStop} 
            disabled={!isSharing} 
            color="#ff3b30" 
          />
        </View>

        <View style={styles.statusContainer}>
          <Text style={styles.statusText}>
            Status: {isSharing ? '📡 Broadcasting...' : '🛑 Stopped'}
          </Text>
          <Text style={styles.counterText}>
            Successful reads: {readCount}
          </Text>
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f2f2f7',
    justifyContent: 'center',
    padding: 20,
  },
  card: {
    backgroundColor: '#ffffff',
    borderRadius: 16,
    padding: 24,
    shadowColor: '#000',
    shadowOpacity: 0.1,
    shadowRadius: 10,
    elevation: 5,
  },
  header: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
    textAlign: 'center',
    color: '#1c1c1e',
  },
  label: {
    fontSize: 14,
    color: '#8e8e93',
    marginBottom: 8,
    fontWeight: '500',
  },
  input: {
    borderWidth: 1,
    borderColor: '#c7c7cc',
    borderRadius: 8,
    padding: 12,
    fontSize: 14,
    marginBottom: 24,
    color: '#1c1c1e',
    backgroundColor: '#fafafa',
    minHeight: 60,
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  statusContainer: {
    marginTop: 32,
    alignItems: 'center',
    paddingTop: 24,
    borderTopWidth: 1,
    borderTopColor: '#e5e5ea',
  },
  statusText: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 8,
    color: '#1c1c1e',
  },
  counterText: {
    fontSize: 16,
    color: '#34c759',
    fontWeight: '500',
  },
});