package ru.descend.kotlintelegrambot.network.serialization.adapter

import ru.descend.kotlintelegrambot.entities.CallbackGame
import ru.descend.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import ru.descend.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackData
import ru.descend.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.CallbackGameButtonType
import ru.descend.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.LoginUrlButtonType
import ru.descend.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.Pay
import ru.descend.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.SwitchInlineQuery
import ru.descend.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.SwitchInlineQueryCurrentChat
import ru.descend.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.Url
import ru.descend.kotlintelegrambot.entities.keyboard.InlineKeyboardButton.WebApp
import ru.descend.kotlintelegrambot.entities.keyboard.LoginUrl
import ru.descend.kotlintelegrambot.entities.keyboard.WebAppInfo
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

internal class InlineKeyboardButtonAdapter : JsonSerializer<InlineKeyboardButton>, JsonDeserializer<InlineKeyboardButton> {

    private class InlineKeyboardButtonDto(
        val text: String,
        val url: String? = null,
        @SerializedName("callback_data") val callbackData: String? = null,
        @SerializedName("callback_game") val callbackGame: CallbackGame? = null,
        @SerializedName("switch_inline_query") val switchInlineQuery: String? = null,
        @SerializedName("switch_inline_query_current_chat") val switchInlineQueryCurrentChat: String? = null,
        val pay: Boolean? = null,
        @SerializedName("web_app") val webApp: WebAppInfo? = null,
        @SerializedName("login_url") val loginUrl: LoginUrl? = null,
    )

    override fun serialize(
        src: InlineKeyboardButton,
        typeOfSrc: Type,
        context: JsonSerializationContext,
    ): JsonElement = when (src) {
        is Url -> context.serialize(src, Url::class.java)
        is CallbackData -> context.serialize(src, CallbackData::class.java)
        is SwitchInlineQuery -> context.serialize(src, SwitchInlineQuery::class.java)
        is SwitchInlineQueryCurrentChat -> context.serialize(src, SwitchInlineQueryCurrentChat::class.java)
        is CallbackGameButtonType -> context.serialize(src, CallbackGameButtonType::class.java)
        is Pay -> context.serialize(src, Pay::class.java)
        is WebApp -> context.serialize(src, WebApp::class.java)
        is LoginUrlButtonType -> context.serialize(src, LoginUrlButtonType::class.java)
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): InlineKeyboardButton {
        val inlineKeyboardButtonDto = context.deserialize<InlineKeyboardButtonDto>(
            json,
            InlineKeyboardButtonDto::class.java,
        )

        return with(inlineKeyboardButtonDto) {
            when {
                url != null -> Url(text, url)
                callbackData != null -> CallbackData(text, callbackData)
                switchInlineQuery != null -> SwitchInlineQuery(text, switchInlineQuery)
                switchInlineQueryCurrentChat != null -> SwitchInlineQueryCurrentChat(
                    text,
                    switchInlineQueryCurrentChat,
                )
                callbackGame != null -> CallbackGameButtonType(text, callbackGame)
                pay != null -> Pay(text)
                webApp != null -> WebApp(text, webApp)
                loginUrl != null -> LoginUrlButtonType(text, loginUrl)
                else -> error("unsupported inline keyboard button $inlineKeyboardButtonDto")
            }
        }
    }
}
