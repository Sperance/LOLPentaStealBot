package ru.descend.bot.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import dev.kord.core.entity.Guild
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.printLog
import ru.descend.bot.toFormatDateTime
import java.io.FileInputStream

const val F_GUILDS = "GUILDS"
const val F_USERS = "USERS"
const val F_MATCHES = "MATCHES"
const val F_PENTAKILLS = "PENTAKILLS"
const val F_PENTASTILLS = "PENTASTILLS"

object FirebaseService {

    var firestore: Firestore

    init {
        printLog("[FirebaseService] Initialized start")
        val serviceAccount = FileInputStream("./credentials.json")

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()

        FirebaseApp.initializeApp(options)

        firestore = FirestoreClient.getFirestore()
        printLog("[FirebaseService] Initialized end")
    }

    fun collectionGuild(guild: Guild, collectionName: String): CollectionReference {
        return firestore.collection(F_GUILDS).document(guild.id.value.toString())
            .collection(collectionName)
    }

    fun collectionGuild(guild: String, collectionName: String): CollectionReference {
        return firestore.collection(F_GUILDS).document(guild)
            .collection(collectionName)
    }

    fun addGuild(guild: Guild): CompleteResult {
        return if (checkDataForCollection(
                firestore.collection(F_GUILDS),
                guild.id.value.toString()
            )
        ) {
            CompleteResult.Error("User is exists with id: ${guild.id.value}")
        } else {
            val guildF = FireGuild()
            guildF.initGuild(guild)
            printLog("[FirebaseService] Creating GUILD with id ${guild.id.value}")
            setDataToCollection(firestore.collection(F_GUILDS), guildF, guildF.id)
        }
    }

    fun addMatchToGuild(guild: String, match: MatchDTO): CompleteResult {
        val mId = match.metadata.matchId

        if (!checkDataForCollection(collectionGuild(guild, F_MATCHES), mId)) {
            printLog("[FirebaseService] Creating Match with GUILD $guild with Match $mId ${match.info.gameMode} time: ${match.info.gameCreation.toFormatDateTime()}")
            return setDataToCollection(collectionGuild(guild, F_MATCHES), FireMatch(match), mId)
        }
        return CompleteResult.Error("Match in guild $guild is exists with id: $mId")
    }

    fun addMatchToGuild(guild: Guild, match: MatchDTO): CompleteResult {
        val mId = match.metadata.matchId

        if (!checkDataForCollection(collectionGuild(guild, F_MATCHES), mId)) {
            printLog("[FirebaseService] Creating Match with GUILD ${guild.id.value} with Match $mId ${match.info.gameMode} time: ${match.info.gameCreation.toFormatDateTime()}")
            return setDataToCollection(collectionGuild(guild, F_MATCHES), FireMatch(match), mId)
        }
        return CompleteResult.Error("Match in guild ${guild.id.value} is exists with id: $mId")
    }

    fun addPerson(guild: Guild, obj: FirePerson): CompleteResult {
        return if (checkDataForCollection(collectionGuild(guild, F_USERS), obj.KORD_id)) {
            CompleteResult.Error("User is exists with id: ${obj.KORD_id}")
        } else {
            printLog("[FirebaseService] Creating Person with GUILD ${guild.id.value} with user ${obj.KORD_id}")
            setDataToCollection(collectionGuild(guild, F_USERS), obj, obj.KORD_id)
        }
    }

    fun checkDataForCollection(collection: CollectionReference, uid: String): Boolean {
        return try {
            val docSnap = collection.document(uid).get().get()
            !(docSnap == null || !docSnap.exists())
        } catch (_: Exception) {
            false
        }
    }

    inline fun <reified T> getArrayFromCollection(collection: CollectionReference, limit: Int = 0): Deferred<ArrayList<T>> {
        return CoroutineScope(Dispatchers.IO).async {
            val dataList = ArrayList<T>()
            for (childSnapshot in withContext(Dispatchers.IO) {
                if (limit == 0) collection.get().get()
                else collection.limit(limit).get().get()
            }.documents) {
                try {
                    val data = childSnapshot.toObject(T::class.java)
                    dataList.add(data)
                } catch (_: Exception) {
                }
            }
            dataList
        }
    }

    private inline fun <reified T> getDataFromCollection(
        collection: CollectionReference,
        uid: String
    ): T? {
        return try {
            val docSnap = collection.document(uid).get().get()
            if (docSnap != null) return docSnap.toObject(T::class.java)
            else {
                return null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getUser(guild: Guild, uid: String): FirePerson? {
        return getDataFromCollection<FirePerson>(collectionGuild(guild, F_USERS), uid)
    }

    fun getGuild(guild: Guild): FireGuild? {
        return getDataFromCollection<FireGuild>(
            firestore.collection(F_GUILDS),
            guild.id.value.toString()
        )
    }

    private fun setDataToCollection(
        collection: CollectionReference,
        data: FireBaseData,
        docName: String? = null
    ): CompleteResult {
        val newDocument =
            if (docName == null) collection.document() else collection.document(docName)
        return try {
            data.SYS_UUID = newDocument.id
            data.SYS_FIRE_PATH = newDocument.path
            collection.document(newDocument.id).set(data)
            CompleteResult.Success()
        } catch (e: Exception) {
            CompleteResult.Error(e.message ?: "")
        }
    }
}