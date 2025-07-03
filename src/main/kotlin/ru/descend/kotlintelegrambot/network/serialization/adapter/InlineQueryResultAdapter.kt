package ru.descend.kotlintelegrambot.network.serialization.adapter

import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult.Article
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult.Audio
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult.CachedAudio
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult.CachedDocument
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult.CachedGif
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult.CachedMpeg4Gif
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult.CachedPhoto
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult.CachedSticker
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult.CachedVideo
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult.CachedVoice
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult.Contact
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult.Document
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult.Game
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult.Gif
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult.Location
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult.Mpeg4Gif
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult.Photo
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult.Venue
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult.Video
import ru.descend.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult.Voice
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

internal class InlineQueryResultAdapter : JsonSerializer<InlineQueryResult> {

    override fun serialize(
        src: InlineQueryResult,
        typeOfSrc: Type,
        context: JsonSerializationContext,
    ): JsonElement = when (src) {
        is Article -> context.serialize(src, Article::class.java)
        is Photo -> context.serialize(src, Photo::class.java)
        is Gif -> context.serialize(src, Gif::class.java)
        is Mpeg4Gif -> context.serialize(src, Mpeg4Gif::class.java)
        is Video -> context.serialize(src, Video::class.java)
        is Audio -> context.serialize(src, Audio::class.java)
        is Voice -> context.serialize(src, Voice::class.java)
        is Document -> context.serialize(src, Document::class.java)
        is Location -> context.serialize(src, Location::class.java)
        is Venue -> context.serialize(src, Venue::class.java)
        is Contact -> context.serialize(src, Contact::class.java)
        is Game -> context.serialize(src, Game::class.java)
        is CachedAudio -> context.serialize(src, CachedAudio::class.java)
        is CachedDocument -> context.serialize(src, CachedDocument::class.java)
        is CachedGif -> context.serialize(src, CachedGif::class.java)
        is CachedMpeg4Gif -> context.serialize(src, CachedMpeg4Gif::class.java)
        is CachedPhoto -> context.serialize(src, CachedPhoto::class.java)
        is CachedSticker -> context.serialize(src, CachedSticker::class.java)
        is CachedVideo -> context.serialize(src, CachedVideo::class.java)
        is CachedVoice -> context.serialize(src, CachedVoice::class.java)
    }
}
