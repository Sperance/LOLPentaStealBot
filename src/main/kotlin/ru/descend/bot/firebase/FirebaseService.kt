package ru.descend.bot.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import dev.kord.core.entity.Guild
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.printLog
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
        return firestore.collection(F_GUILDS).document(guild.id.value.toString()).collection(collectionName)
    }

    fun collectionGuildUser(guild: Guild, user: FirePerson, collectionName: String): CollectionReference {
        return collectionGuild(guild, F_USERS).document(user.KORD_id).collection(collectionName)
    }

    suspend fun addGuild(guild: Guild): CompleteResult {
        return if (checkDataForCollection(firestore.collection(F_GUILDS), guild.id.value.toString())) {
            CompleteResult.Error("User is exists with id: ${guild.id.value}")
        } else {
            val guildF = FireGuild()
            guildF.initGuild(guild)
            printLog("[FirebaseService] Creating GUILD with id ${guild.id.value}")
            setDataToCollection(firestore.collection(F_GUILDS), guildF, guildF.id)
        }
    }

    suspend fun addMatchToUser(guild: Guild, user: FirePerson, match: MatchDTO) : CompleteResult {
        val mId = match.metadata.matchId

        if (user.matchIDs.contains(mId)) {
            return CompleteResult.Success("In user ${user.LOL_puuid} match $mId already loaded")
        }

        printLog("[FirebaseService] Creating Match with GUILD ${guild.id.value} with user ${user.KORD_id} with Match $mId")

        val newMatch = FireMatch.initMatch(user.LOL_puuid, match)
        return if (newMatch!= null) {
            user.matchIDs.add(newMatch.matchId)
            when (val saved = user.fireSaveData()){
                is CompleteResult.Error -> CompleteResult.Error(saved.errorText)
                is CompleteResult.Success -> setDataToCollection(collectionGuildUser(guild, user, F_MATCHES), newMatch, mId)
            }
        } else {
            CompleteResult.Error("Not find match with id $mId")
        }
    }

    suspend fun addPentaSteal(guild: Guild, user: FirePerson, obj: FirePSteal): CompleteResult {
        printLog("[FirebaseService] Creating PentaSteal with GUILD ${guild.id.value} with user ${user.KORD_id}")
        return setDataToCollection(collectionGuildUser(guild, user, F_PENTASTILLS), obj)
    }

    suspend fun addPentaKill(guild: Guild, user: FirePerson, obj: FirePKill): CompleteResult {
        printLog("[FirebaseService] Creating PentaKill with GUILD ${guild.id.value} with user ${user.KORD_id}")
        return setDataToCollection(collectionGuildUser(guild, user, F_PENTAKILLS), obj)
    }

    suspend fun addPerson(guild: Guild, obj: FirePerson): CompleteResult {
        return if (checkDataForCollection(collectionGuild(guild, F_USERS), obj.KORD_id)) {
            CompleteResult.Error("User is exists with id: ${obj.KORD_id}")
        } else {
            printLog("[FirebaseService] Creating Person with GUILD ${guild.id.value} with user ${obj.KORD_id}")
            setDataToCollection(collectionGuild(guild, F_USERS), obj, obj.KORD_id)
        }
    }

    suspend fun checkDataForCollection(collection: CollectionReference, uid: String): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        withContext(Dispatchers.IO) {
            collection.get().get().documents.forEach {
                if (it.id == uid) {
                    deferred.complete(true)
                    return@forEach
                }
            }
            deferred.complete(false)
        }
        return deferred.await()
    }

    inline fun <reified T> getArrayFromCollection(collection: CollectionReference): ArrayList<T> {
        val dataList = ArrayList<T>()
        for (childSnapshot in collection.get().get().documents) {
            try {
                val data = childSnapshot.toObject(T::class.java)
                dataList.add(data)
            } catch (_: Exception) { }
        }
        return dataList
    }

    inline fun <reified T> getDataFromCollection(collection: CollectionReference, uid: String): T? {
        for (childSnapshot in collection.get().get().documents) {
            if (childSnapshot.id == uid){
                return try {
                    childSnapshot.toObject(T::class.java)
                } catch (e: Exception) {
                    println("Error: ${e.localizedMessage}")
                    null
                }
            }
        }
        return null
    }

    fun getUser(guild: Guild, uid: String) : FirePerson? {
        return getDataFromCollection<FirePerson>(collectionGuild(guild, F_USERS), uid)
    }

    fun getGuild(guild: Guild) : FireGuild? {
        return getDataFromCollection<FireGuild>(firestore.collection(F_GUILDS), guild.id.value.toString())
    }

    suspend fun setDataToCollection(
        collection: CollectionReference,
        data: FireBaseData,
        docName: String? = null
    ): CompleteResult {
        val deferred = CompletableDeferred<CompleteResult>()
        val newDocument = if (docName == null) collection.document() else collection.document(docName)
        withContext(Dispatchers.IO) {
            val result = firestore.runTransaction { transaction ->
                try {
                    data.SYS_UUID = newDocument.id
                    data.SYS_FIRE_PATH = newDocument.path
                    transaction.set(newDocument, data)
                    CompleteResult.Success()
                } catch (e: Exception) {
                    CompleteResult.Error(e.message ?: "")
                }
            }.get()
            deferred.complete(result)
        }
        return deferred.await()
    }
}