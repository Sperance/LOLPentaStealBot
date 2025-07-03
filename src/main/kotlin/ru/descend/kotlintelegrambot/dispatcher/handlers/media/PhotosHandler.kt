package com.github.kotlintelegrambot.dispatcher.handlers.media

import com.github.kotlintelegrambot.dispatcher.handlers.HandlePhotos
import ru.descend.kotlintelegrambot.entities.Message
import ru.descend.kotlintelegrambot.entities.Update
import ru.descend.kotlintelegrambot.entities.files.PhotoSize

class PhotosHandler(
    handlePhotos: HandlePhotos,
) : MediaHandler<List<PhotoSize>>(
    handlePhotos,
    ::mapMessageToPhotos,
    ::isUpdatePhotos,
) {
    private companion object {
        private fun mapMessageToPhotos(message: Message): List<PhotoSize> {
            val photos = message.photo
            checkNotNull(photos)
            return photos
        }

        private fun isUpdatePhotos(update: Update): Boolean {
            val photos = update.message?.photo

            return !photos.isNullOrEmpty()
        }
    }
}
