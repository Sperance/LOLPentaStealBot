package ru.descend.kotlintelegrambot.entities

import ru.descend.kotlintelegrambot.entities.payments.PreCheckoutQuery
import ru.descend.kotlintelegrambot.entities.payments.ShippingQuery
import ru.descend.kotlintelegrambot.entities.polls.Poll
import ru.descend.kotlintelegrambot.entities.polls.PollAnswer
import ru.descend.kotlintelegrambot.types.ConsumableObject
import ru.descend.kotlintelegrambot.types.DispatchableObject
import com.google.gson.annotations.SerializedName as Name

data class Update constructor(
    @Name("update_id") val updateId: Long,
    val message: Message? = null,
    @Name("edited_message") val editedMessage: Message? = null,
    @Name("channel_post") val channelPost: Message? = null,
    @Name("edited_channel_post") val editedChannelPost: Message? = null,
    @Name("inline_query") val inlineQuery: InlineQuery? = null,
    @Name("chosen_inline_result") val chosenInlineResult: ChosenInlineResult? = null,
    @Name("callback_query") val callbackQuery: CallbackQuery? = null,
    @Name("shipping_query") val shippingQuery: ShippingQuery? = null,
    @Name("pre_checkout_query") val preCheckoutQuery: PreCheckoutQuery? = null,
    @Name("poll") val poll: Poll? = null,
    @Name("poll_answer") val pollAnswer: PollAnswer? = null,
    @Name("chat_join_request") val chatJoinRequest: ChatJoinRequest? = null,
    @Name("my_chat_member") val myChatMember: ChatMemberUpdated? = null,
    @Name("chat_member") val chatMember: ChatMemberUpdated? = null,
) : DispatchableObject, ConsumableObject()

/**
 * Generate list of key-value from start payload.
 * For more info {@link https://core.telegram.org/bots#deep-linking}
 */
fun Update.getStartPayload(delimiter: String = "-"): List<Pair<String, String>> {
    return message?.let {
        val parameters = it.text?.substringAfter("start ", "")
        if (parameters.isNullOrEmpty()) {
            return emptyList()
        }

        val split = parameters.split("&")
        split.map {
            val keyValue = it.split(delimiter)
            Pair(keyValue[0], keyValue[1])
        }
    } ?: emptyList()
}
