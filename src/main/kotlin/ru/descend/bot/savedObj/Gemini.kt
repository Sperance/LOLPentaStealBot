package ru.descend.bot.savedObj

import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import ru.descend.bot.catchToken

object Gemini {

    private val model = GenerativeModel(
        modelName = "gemini-pro",
        apiKey = catchToken()[2]
    )

    suspend fun generateForText(textContent: String) =
        try {
            model.generateContent(textContent).text ?: ""
        } catch (e: Exception) {
            ""
        }
}