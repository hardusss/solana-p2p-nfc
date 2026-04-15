package com.nextvibe.solanap2pnfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import java.util.Arrays

/**
 * Host-based Card Emulation (HCE) service.
 * Emulates an NFC Forum Type 4 Tag to share an NDEF message (a URL) with another device.
 */
class NdefHostApduService : HostApduService() {

    // Global state used as a bridge to communicate with the Expo/React Native JS side.
    companion object {
        var urlToShare: String = "https://nextvibe.io"
        var onReadListener: (() -> Unit)? = null
    }

    // Standard APDU commands defined by the NFC Forum Type 4 Tag specification.
    // The reading device sends these to discover, select, and read the NDEF data.
    private val APDU_SELECT = byteArrayOf(
        0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(),
        0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(), 0x85.toByte(),
        0x01.toByte(), 0x01.toByte(), 0x00.toByte()
    )
    private val CAPABILITY_CONTAINER_OK = byteArrayOf(
        0x00.toByte(), 0xA4.toByte(), 0x00.toByte(), 0x0C.toByte(), 0x02.toByte(),
        0xE1.toByte(), 0x03.toByte()
    )
    private val READ_CAPABILITY_CONTAINER = byteArrayOf(
        0x00.toByte(), 0xB0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0F.toByte()
    )
    private val NDEF_SELECT_OK = byteArrayOf(
        0x00.toByte(), 0xA4.toByte(), 0x00.toByte(), 0x0C.toByte(), 0x02.toByte(),
        0xE1.toByte(), 0x04.toByte()
    )
    private val NDEF_READ_BINARY = byteArrayOf(0x00.toByte(), 0xB0.toByte())

    // Standard APDU status words
    private val RESP_OK = byteArrayOf(0x90.toByte(), 0x00.toByte())
    private val RESP_FAIL = byteArrayOf(0x6A.toByte(), 0x82.toByte())

    // State machine flags to track the reader's progression
    private var appSelected = false
    private var ccSelected = false
    private var ndefSelected = false

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        // Step 1: Reader selects the NDEF application using its AID (D2760000850101)
        if (Arrays.equals(APDU_SELECT, commandApdu)) {
            appSelected = true
            ccSelected = false
            ndefSelected = false
            return RESP_OK
        }
        
        // Step 2: Reader selects the Capability Container (CC) file
        if (Arrays.equals(CAPABILITY_CONTAINER_OK, commandApdu)) {
            if (appSelected) ccSelected = true
            return RESP_OK
        }

        // Step 3: Reader reads the CC file
        if (Arrays.equals(READ_CAPABILITY_CONTAINER, commandApdu) && ccSelected) {
            // This hardcoded byte array tells the reader: 
            // "I am an NDEF tag, max size is ~65KB, read access granted, write access denied."
            val ccFile = byteArrayOf(
                0x00, 0x0F, 0x20, 0x00, 0x3B, 0x00, 0x34, 0x04, 0x06,
                0xE1.toByte(), 0x04.toByte(), 0x00, 0xFF.toByte(), 0x00, 0xFF.toByte()
            )
            return ccFile + RESP_OK
        }

        // Step 4: Reader selects the actual NDEF message file
        if (Arrays.equals(NDEF_SELECT_OK, commandApdu)) {
            if (appSelected) {
                ndefSelected = true
                // The reader passed all validations and is about to extract the URL.
                // It's the safest moment to trigger the JS callback.
                onReadListener?.invoke()
            }
            return RESP_OK
        }

        // Step 5: Reader requests the NDEF payload (Read Binary)
        if (commandApdu.size >= 2 &&
            commandApdu[0] == NDEF_READ_BINARY[0] &&
            commandApdu[1] == NDEF_READ_BINARY[1] &&
            ndefSelected
        ) {
            // The reader might read in chunks. We extract the starting offset from bytes 2 and 3.
            // (commandApdu[2] shl 8) shifts the high byte 8 bits to the left to form a 16-bit integer.
            val offset = ((commandApdu[2].toInt() and 0xFF) shl 8) or (commandApdu[3].toInt() and 0xFF)
            val length = commandApdu[4].toInt() and 0xFF

            val ndefMessage = createNdefMessage(urlToShare)
            
            // The NDEF specification requires prepending a 2-byte length (NLEN) before the actual message.
            val nlen = byteArrayOf(
                (ndefMessage.size shr 8).toByte(),
                (ndefMessage.size and 0xFF).toByte()
            )
            val fullNdef = nlen + ndefMessage

            // Handle offset and chunking in case the reader's buffer is smaller than our message
            if (offset >= fullNdef.size) return RESP_FAIL
            val readLength = Math.min(length, fullNdef.size - offset)
            val response = ByteArray(readLength)
            System.arraycopy(fullNdef, offset, response, 0, readLength)
            
            return response + RESP_OK
        }

        return RESP_FAIL
    }

    /**
     * Constructs a standard NDEF URI Record.
     */
    private fun createNdefMessage(url: String): ByteArray {
        val urlBytes = url.toByteArray(Charsets.UTF_8)
        val payload = ByteArray(urlBytes.size + 1)
        
        // Identifier Code: 0x00 means no prefix. The URL is read entirely as provided.
        // (For example, 0x01 would automatically prepend "http://www." and 0x02 "https://www.")
        payload[0] = 0x00
        System.arraycopy(urlBytes, 0, payload, 1, urlBytes.size)

        val record = ByteArray(payload.size + 4)
        // Record Header (0xD1): MB=1, ME=1 (Message Begin/End), SR=1 (Short Record), TNF=0x01 (Well-Known type)
        record[0] = 0xD1.toByte()
        // Type Length (1 byte, because the type 'U' is just one character)
        record[1] = 0x01.toByte()
        // Payload Length
        record[2] = payload.size.toByte()
        // Record Type (0x55 represents the ASCII character 'U' for URI)
        record[3] = 0x55.toByte()
        
        System.arraycopy(payload, 0, record, 4, payload.size)
        return record
    }

    override fun onDeactivated(reason: Int) {
        // Reset the state machine when the reading device disconnects
        appSelected = false
        ccSelected = false
        ndefSelected = false
    }
}