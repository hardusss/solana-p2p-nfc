package com.nextvibe.solanap2pnfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import java.util.Arrays

/**
 * Host-based Card Emulation (HCE) service.
 * Emulates an NFC Forum Type 4 Tag to share an NDEF message (a URL) with another device.
 */
class NdefHostApduService : HostApduService() {

    companion object {
        var urlToShare: String = "https://google.com"
        var onReadListener: (() -> Unit)? = null
    }

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

    private val RESP_OK = byteArrayOf(0x90.toByte(), 0x00.toByte())
    private val RESP_FAIL = byteArrayOf(0x6A.toByte(), 0x82.toByte())

    private var appSelected = false
    private var ccSelected = false
    private var ndefSelected = false

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        if (Arrays.equals(APDU_SELECT, commandApdu)) {
            appSelected = true
            ccSelected = false
            ndefSelected = false
            return RESP_OK
        }
        
        if (Arrays.equals(CAPABILITY_CONTAINER_OK, commandApdu)) {
            if (appSelected) ccSelected = true
            return RESP_OK
        }

        if (Arrays.equals(READ_CAPABILITY_CONTAINER, commandApdu) && ccSelected) {
            val ccFile = byteArrayOf(
                0x00.toByte(), 0x0F.toByte(), // CCLEN
                0x20.toByte(), // Mapping Version 2.0
                0x00.toByte(), 0xFF.toByte(), // MLe: 255 bytes (Max R-APDU)
                0x00.toByte(), 0xFF.toByte(), // MLc: 255 bytes (Max C-APDU)
                0x04.toByte(), 0x06.toByte(), // T & L of NDEF File Control TLV
                0xE1.toByte(), 0x04.toByte(), // File Identifier
                0x03.toByte(), 0xE8.toByte(), // Max NDEF Size = 1000 bytes (0x03E8)
                0x00.toByte(), // Read Access = granted
                0xFF.toByte()  // Write Access = denied
            )
            return ccFile + RESP_OK
        }

        if (Arrays.equals(NDEF_SELECT_OK, commandApdu)) {
            if (appSelected) {
                ndefSelected = true
                onReadListener?.invoke()
            }
            return RESP_OK
        }

        if (commandApdu.size >= 2 &&
            commandApdu[0] == NDEF_READ_BINARY[0] &&
            commandApdu[1] == NDEF_READ_BINARY[1] &&
            ndefSelected
        ) {
            val offset = ((commandApdu[2].toInt() and 0xFF) shl 8) or (commandApdu[3].toInt() and 0xFF)
            var length = if (commandApdu.size >= 5) commandApdu[4].toInt() and 0xFF else 0
            
            if (length == 0) {
                length = 256
            }

            val ndefMessage = createNdefMessage(urlToShare)
            val nlen = byteArrayOf(
                (ndefMessage.size shr 8).toByte(),
                (ndefMessage.size and 0xFF).toByte()
            )
            val fullNdef = nlen + ndefMessage

            if (offset >= fullNdef.size) return RESP_FAIL
            val readLength = Math.min(length, fullNdef.size - offset)
            val response = ByteArray(readLength)
            System.arraycopy(fullNdef, offset, response, 0, readLength)
            
            return response + RESP_OK
        }

        return RESP_FAIL
    }

    private fun createNdefMessage(url: String): ByteArray {
        val urlBytes = url.toByteArray(Charsets.UTF_8)
        val payload = ByteArray(urlBytes.size + 1)

        payload[0] = 0x00
        System.arraycopy(urlBytes, 0, payload, 1, urlBytes.size)

        val record = ByteArray(payload.size + 4)
        record[0] = 0xD1.toByte()
        record[1] = 0x01.toByte()
        record[2] = payload.size.toByte()
        record[3] = 0x55.toByte()
        
        System.arraycopy(payload, 0, record, 4, payload.size)
        return record
    }

    override fun onDeactivated(reason: Int) {
        appSelected = false
        ccSelected = false
        ndefSelected = false
    }
}