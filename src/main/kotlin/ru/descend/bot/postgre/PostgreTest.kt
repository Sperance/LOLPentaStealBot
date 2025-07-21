package ru.descend.bot.postgre

import com.cjcrafter.openai.OpenAI
import com.cjcrafter.openai.chat.ChatMessage
import com.cjcrafter.openai.chat.ChatRequest
import com.cjcrafter.openai.chat.ChatUser
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.junit.Test
import ru.descend.bot.datas.Toplols
import ru.descend.bot.datas.getData
import ru.descend.bot.datas.getDataOne
import ru.descend.bot.datas.update
import ru.descend.bot.fromHexInt
import ru.descend.bot.generateAIText
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.printLog
import ru.descend.bot.sqlData
import ru.descend.bot.to1Digits
import ru.descend.bot.toHexInt
import ru.gildor.coroutines.okhttp.await
import java.time.LocalDate

class PostgreTest {

    @Test
    fun testConvertations() {
        val str = "[S+:DAMAGE:28.4];"
        val coded = str.toHexInt()
        println("codded: $coded")
        println("encoded: ${coded.fromHexInt()}")
    }

    @Test
    fun testConvertations2() {
        runBlocking {
            val lolobj = LOLs().getDataOne({ tbl_lols.id eq 14 })!!
            printLog("DATA: ${lolobj.f_aram_last_key.fromHexInt()}")
//            lolobj.f_aram_kills3 = 5.2
//            lolobj.f_aram_last_key = "[S+:32:DAMAGE];".toHexInt()
//            lolobj.update()
        }
    }

    @Test
    fun test_coerce() {
        val start = 38.25
        val end = 50.0
        println("COE 1: ${(start / end).coerceIn(10.0, 50.0)}")
        println("COE 1: ${(start / end).coerceIn(0.0, 50.0)}")
        println("COE 1: ${(start / end).coerceIn(0.0, 100.0)}")
    }

    @Test
    fun test_chatgpt() {
        val openai = OpenAI.builder()
            .apiKey("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")
            .build()

        val messages: MutableList<ChatMessage> = ArrayList()

        // Here you can change the model's settings, add tools, and more.
        val request = ChatRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(messages)
            .build()

        messages.add(ChatMessage(ChatUser.USER, "How are you?"))
        val response = openai.createChatCompletion(request)
        println("Generating Response...")
        println(response[0].message.content)
        // Make sure to add the response to the messages list!
        messages.add(response[0].message)
    }

    @Test
    fun test_proxy_older() {
        runBlocking {
//            val url = "https://api.openai.com/v1/chat/completions"
            val url = "https://api.mistral.ai/chat/completions"
//            val url = "https://descend-oai-proxy5.hf.space/proxy/openai/v1/chat/completions"
            val requestText = "Hello? how are you?"
            val JSON = "application/json; charset=utf-8".toMediaType()
            val body = RequestBody.create(JSON, "{\n" +
                    "        \"model\": \"gpt-3.5-turbo\",\n" +
                    "        \"messages\": [{\"role\": \"user\", \"content\": \"$requestText\"}]\n" +
                    "    }")
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()
            val response = OkHttpClient().newCall(request).await()
            println("code: ${response.code}")
            println("message: ${response.message}")
            println("result: ${response.body?.string()}")
        }
    }

    @Test
    fun test_AI_context() {
        runBlocking {
            val generatedText = generateAIText("Напиши прикольный факт про игру League of Legends")
            val resultedText = "**Рубрика: интересные факты**\n\n$generatedText"
            println(resultedText)
        }
    }

    @Test
    fun test_birthday_parse() {
        val dateValue = "03091900_2023"

        val valueDay = dateValue.substring(0..1).toInt()
        val valueMonth = dateValue.substring(2..3).toInt()
        val valueYear = dateValue.substring(4..7).toInt()
        printLog("d$valueDay m$valueMonth y$valueYear")

        val curDate = LocalDate.of(2000, valueMonth, valueDay)
        printLog(curDate.dayOfMonth)
        printLog(curDate.monthValue)
        printLog(curDate.year)

        val curSysDate = LocalDate.of(2000, LocalDate.now().monthValue, LocalDate.now().dayOfMonth + 1)
        if (curDate < curSysDate) printLog("low")
        if (curDate > curSysDate) printLog("great")
        if (curDate == curSysDate) printLog("eq")
    }
}