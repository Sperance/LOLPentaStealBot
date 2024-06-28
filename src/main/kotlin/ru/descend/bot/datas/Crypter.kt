package ru.descend.bot.datas

import java.security.Key
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec
import javax.crypto.spec.SecretKeySpec

const val DSC_PS = "MTE2NzE0MTUxNzYzMTE2ODU0Mg.GdmE_M.CWl4aktgkUs4lj0OLBfUCzYKtOB-mjAmW0-bos"

fun encrypt(input: String, password: String): ByteArray {
    val c = Cipher.getInstance("DES")
    val kf = SecretKeyFactory.getInstance("DES")
    val keySpec = DESKeySpec(password.toByteArray())
    val key: Key? = kf.generateSecret(keySpec)
    c.init(Cipher.ENCRYPT_MODE, key)
    return c.doFinal(input.toByteArray())
}

fun decrypt(input: ByteArray, password: String): ByteArray {
    val c = Cipher.getInstance("DES")
    val kf = SecretKeyFactory.getInstance("DES")
    val keySpec = DESKeySpec(password.toByteArray())
    val key: Key? = kf.generateSecret(keySpec)
    c.init(Cipher.DECRYPT_MODE, key)
    return c.doFinal(input)
}

object AESUtils {
    private val keyValue: ByteArray = byteArrayOf(
        'Я'.code.toByte(),
        'o'.code.toByte(),
        'd'.code.toByte(),
        '-'.code.toByte(),
        'N'.code.toByte(),
        'g'.code.toByte(),
        '№'.code.toByte(),
        'f'.code.toByte(),
        'f'.code.toByte(),
        '2'.code.toByte(),
        'i'.code.toByte(),
        'r'.code.toByte(),
        's'.code.toByte(),
        '1'.code.toByte(),
        'o'.code.toByte(),
        'П'.code.toByte()
    )


    @Throws(java.lang.Exception::class)
    fun encrypt(cleartext: String): String {
        var rawKey = rawKey
        var result = encrypt(rawKey, cleartext.toByteArray())
        return toHex(result)
    }

    @Throws(java.lang.Exception::class)
    fun decrypt(encrypted: String): String {
        var enc = toByte(encrypted)
        var result = decrypt(enc)
        return String(result)
    }

    @get:Throws(java.lang.Exception::class)
    private val rawKey: ByteArray
        get() {
            var key: SecretKey = SecretKeySpec(keyValue, "AES")
            var raw = key.encoded
            return raw
        }

    @Throws(java.lang.Exception::class)
    private fun encrypt(raw: ByteArray, clear: ByteArray): ByteArray {
        var skeySpec: SecretKey = SecretKeySpec(raw, "AES")
        var cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
        var encrypted = cipher.doFinal(clear)
        return encrypted
    }

    @Throws(java.lang.Exception::class)
    private fun decrypt(encrypted: ByteArray): ByteArray {
        var skeySpec: SecretKey = SecretKeySpec(keyValue, "AES")
        var cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, skeySpec)
        var decrypted = cipher.doFinal(encrypted)
        return decrypted
    }

    fun toByte(hexString: String): ByteArray {
        var len = hexString.length / 2
        var result = ByteArray(len)
        for (i in 0 until len) result[i] = hexString.substring(2 * i, 2 * i + 2).toInt(16).toByte()
        return result
    }

    fun toHex(buf: ByteArray?): String {
        if (buf == null) return ""
        var result = StringBuffer(2 * buf.size)
        for (i in buf.indices) {
            appendHex(result, buf[i])
        }
        return result.toString()
    }

    private const val HEX = "0123456789ABCDEF"

    private fun appendHex(sb: StringBuffer, b: Byte) {
        sb.append(HEX[b.toInt() shr 4 and 0x0f]).append(HEX[b.toInt() and 0x0f])
    }
}