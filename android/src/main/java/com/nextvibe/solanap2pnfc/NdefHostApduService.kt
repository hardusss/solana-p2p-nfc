package com.nextvibe.solanap2pnfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.os.Handler
import android.os.Looper

/**
 * Host-based Card Emulation (HCE) service.
 * Emulates an NFC Forum Type 4 Tag to share an NDEF message with another device.
 *
 * APDU flow:
 *  1. SELECT NDEF Application (by AID)
 *  2. SELECT CC File
 *  3. READ BINARY CC File → returns Capability Container
 *  4. SELECT NDEF File
 *  5. READ BINARY NDEF File → returns NDEF message with 2-byte length prefix
 */
class NdefHostApduService : HostApduService() {

    companion object {
        var urlToShare: String = "https://google.com"
        var onReadListener: (() -> Unit)? = null
    }

    // Status words
    private val RESP_OK   = byteArrayOf(0x90.toByte(), 0x00.toByte())
    private val RESP_FAIL = byteArrayOf(0x6A.toByte(), 0x82.toByte())

    // File identifiers
    private val CC_FILE_ID   = byteArrayOf(0xE1.toByte(), 0x03.toByte())
    private val NDEF_FILE_ID = byteArrayOf(0xE1.toByte(), 0x04.toByte())

    // NDEF Application AID: D2760000850101
    private val NDEF_AID = byteArrayOf(
        0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x85.toByte(), 0x01.toByte(), 0x01.toByte()
    )

    private var appSelected  = false
    private var ccSelected   = false
    private var ndefSelected = false

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        if (commandApdu.size < 4) return RESP_FAIL

        val cla = commandApdu[0]
        val ins = commandApdu[1]
        val p1  = commandApdu[2]
        val p2  = commandApdu[3]

        // ── SELECT (INS = 0xA4) ──────────────────────────────────────────────
        if (ins == 0xA4.toByte()) {

            // SELECT by AID (P1=0x04, P2=0x00) → SELECT NDEF Application
            if (p1 == 0x04.toByte() && p2 == 0x00.toByte()) {
                val lc = if (commandApdu.size >= 5) commandApdu[4].toInt() and 0xFF else 0
                if (commandApdu.size >= 5 + lc && lc == NDEF_AID.size) {
                    val aid = commandApdu.copyOfRange(5, 5 + lc)
                    if (aid.contentEquals(NDEF_AID)) {
                        appSelected  = true
                        ccSelected   = false
                        ndefSelected = false
                        return RESP_OK
                    }
                }
                return RESP_FAIL
            }

            // SELECT by File ID (P1=0x00, P2=0x0C) → SELECT CC or NDEF file
            if (p1 == 0x00.toByte() && p2 == 0x0C.toByte()) {
                if (!appSelected) return RESP_FAIL
                val lc = if (commandApdu.size >= 5) commandApdu[4].toInt() and 0xFF else 0
                if (commandApdu.size < 5 + lc || lc < 2) return RESP_FAIL

                val fileId = commandApdu.copyOfRange(5, 5 + lc)
                return when {
                    fileId.contentEquals(CC_FILE_ID) -> {
                        ccSelected   = true
                        ndefSelected = false
                        RESP_OK
                    }
                    fileId.contentEquals(NDEF_FILE_ID) -> {
                        ndefSelected = true
                        ccSelected   = false
                        RESP_OK
                    }
                    else -> RESP_FAIL
                }
            }

            return RESP_FAIL
        }

        // ── READ BINARY (INS = 0xB0) ─────────────────────────────────────────
        if (ins == 0xB0.toByte()) {
            val offset = ((p1.toInt() and 0xFF) shl 8) or (p2.toInt() and 0xFF)
            var length = if (commandApdu.size >= 5) commandApdu[4].toInt() and 0xFF else 0
            if (length == 0) length = 256 // Le=0x00 means 256 in short APDU

            // READ CC File
            if (ccSelected) {
                val cc = buildCapabilityContainer()
                return readBinary(cc, offset, length)
            }

            // READ NDEF File
            if (ndefSelected) {
                val ndefMessage = createNdefMessage(urlToShare)
                // NDEF file = 2-byte NLEN + NDEF message
                val nlen = byteArrayOf(
                    (ndefMessage.size shr 8).toByte(),
                    (ndefMessage.size and 0xFF).toByte()
                )
                val fullNdef = nlen + ndefMessage
                val result = readBinary(fullNdef, offset, length)

                // Fire the listener only after the last chunk is sent
                if (offset + length >= fullNdef.size) {
                    Handler(Looper.getMainLooper()).post {
                        onReadListener?.invoke()
                    }
                }
                return result
            }

            return RESP_FAIL
        }

        return RESP_FAIL
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Reads [length] bytes from [data] starting at [offset].
     * Returns the slice + 0x9000, or 0x6A82 on out-of-bounds.
     */
    private fun readBinary(data: ByteArray, offset: Int, length: Int): ByteArray {
        if (offset >= data.size) return RESP_FAIL
        val readLen = minOf(length, data.size - offset)
        return data.copyOfRange(offset, offset + readLen) + RESP_OK
    }

    /**
     * Builds the 15-byte Capability Container (CC) file.
     *
     * Layout (NFC Forum T4T spec, section 5.1):
     *   [0-1]  CCLEN   = 0x000F (15)
     *   [2]    MappingVersion = 0x20 (v2.0)
     *   [3-4]  MLe     = 0x00FF (max 255 bytes per R-APDU data)
     *   [5-6]  MLc     = 0x00FF (max 255 bytes per C-APDU data)
     *   [7]    T       = 0x04 (NDEF File Control TLV tag)
     *   [8]    L       = 0x06 (length of V field)
     *   [9-10] File ID = E1 04
     *   [11-12]Max NDEF size = 0x03E8 (1000 bytes)
     *   [13]   Read Access  = 0x00 (open)
     *   [14]   Write Access = 0xFF (deny)
     */
    private fun buildCapabilityContainer(): ByteArray = byteArrayOf(
        0x00, 0x0F,               // CCLEN = 15
        0x20,                     // Mapping Version 2.0
        0x00, 0xFF.toByte(),      // MLe = 255
        0x00, 0xFF.toByte(),      // MLc = 255
        0x04, 0x06,               // T=0x04, L=0x06
        0xE1.toByte(), 0x04,      // File Identifier = E104
        0x03.toByte(), 0xE8.toByte(), // Max NDEF size = 1000
        0x00,                     // Read access: open
        0xFF.toByte()             // Write access: deny
    )

    /**
     * Builds a minimal NDEF record containing a URI (type 0x55).
     *
     * NDEF record structure (short record, MB=1, ME=1, SR=1):
     *   [0] Header byte: 0xD1 (MB|ME|SR|TNF=0x01 Well-Known)
     *   [1] Type Length = 1
     *   [2] Payload Length (1 byte because SR=1)
     *   [3] Type = 0x55 ('U' = URI record type)
     *   [4] URI identifier code (0x00 = no prefix / full URI)
     *   [5..] URI bytes
     *
     * Note: identifier code 0x00 means the URI is stored in full.
     * Use 0x04 if you want to strip the "https://" prefix automatically,
     * but 0x00 is safest for custom URI schemes like "solana:".
     */
    private fun createNdefMessage(uri: String): ByteArray {
        val uriBytes  = uri.toByteArray(Charsets.UTF_8)
        // payload = identifier code byte + URI bytes
        val payload   = byteArrayOf(0x00) + uriBytes

        check(payload.size <= 255) {
            "URI too long for a Short Record NDEF message (max 254 URI bytes)"
        }

        return byteArrayOf(
            0xD1.toByte(),            // NDEF header: MB|ME|SR|TNF=Well-Known
            0x01.toByte(),            // Type Length = 1
            payload.size.toByte(),    // Payload Length (≤255, safe because of check above)
            0x55.toByte()             // Type = 'U' (URI)
        ) + payload
    }

    override fun onDeactivated(reason: Int) {
        appSelected  = false
        ccSelected   = false
        ndefSelected = false
    }
}