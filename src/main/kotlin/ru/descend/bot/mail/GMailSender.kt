package ru.descend.bot.mail

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.AccessController
import java.security.PrivilegedAction
import java.security.Provider
import java.security.Security
import java.util.Properties
import javax.activation.DataHandler
import javax.activation.DataSource
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


class GMailSender(private val user: String, private val password: String) :
    javax.mail.Authenticator() {
    private val mailhost = "smtp.gmail.com"
    private val session: Session

    init {
        val props = Properties()
        props.setProperty("mail.transport.protocol", "smtp")
        props.setProperty("mail.host", mailhost)
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.port"] = "465"
        props["mail.smtp.socketFactory.port"] = "465"
        props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
        props["mail.smtp.socketFactory.fallback"] = "false"
        props.setProperty("mail.smtp.quitwait", "false")
        session = Session.getDefaultInstance(props, this)
    }

    override fun getPasswordAuthentication(): PasswordAuthentication {
        return PasswordAuthentication(user, password)
    }

    @Synchronized
    @Throws(Exception::class)
    fun sendMail(subject: String?, body: String, sender: String?, recipients: String) {
        try {
            val message = MimeMessage(session)
            val handler = DataHandler(ByteArrayDataSource(body.toByteArray(), "text/plain"))
            message.sender = InternetAddress(sender)
            message.subject = subject
            message.dataHandler = handler
            if (recipients.indexOf(',') > 0) message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(recipients)
            ) else message.setRecipient(Message.RecipientType.TO, InternetAddress(recipients))
            Transport.send(message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    inner class ByteArrayDataSource : DataSource {
        private var data: ByteArray
        private var type: String? = null

        constructor(data: ByteArray, type: String?) : super() {
            this.data = data
            this.type = type
        }

        constructor(data: ByteArray) : super() {
            this.data = data
        }

        fun setType(type: String?) {
            this.type = type
        }

        override fun getInputStream(): InputStream {
            return ByteArrayInputStream(data)
        }

        override fun getOutputStream(): OutputStream {
            throw IOException("Not Supported")
        }

        override fun getContentType(): String {
            return type ?: "application/octet-stream"
        }

        override fun getName(): String {
            return "ByteArrayDataSource"
        }
    }

//    companion object {
//        init {
//            Security.addProvider(JSSEProvider())
//        }
//    }
}


class JSSEProvider : Provider("HarmonyJSSE", 1.0, "Harmony JSSE Provider") {
    init {
        AccessController.doPrivileged<Void>(PrivilegedAction {
            put(
                "SSLContext.TLS",
                "org.apache.harmony.xnet.provider.jsse.SSLContextImpl"
            )
            put("Alg.Alias.SSLContext.TLSv1", "TLS")
            put(
                "KeyManagerFactory.X509",
                "org.apache.harmony.xnet.provider.jsse.KeyManagerFactoryImpl"
            )
            put(
                "TrustManagerFactory.X509",
                "org.apache.harmony.xnet.provider.jsse.TrustManagerFactoryImpl"
            )
            null
        })
    }
}