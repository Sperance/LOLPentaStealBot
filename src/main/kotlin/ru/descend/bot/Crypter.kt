package ru.descend.bot

import java.security.Key
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec

const val MAIN_ROLE_NAME = "Пентастилька"
const val SECOND_ROLE_NAME = "Демасия"
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