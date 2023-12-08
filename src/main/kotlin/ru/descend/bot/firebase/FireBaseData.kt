package ru.descend.bot.firebase

import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.annotation.Exclude

sealed class CompleteResult {
    class Success(val successText: String? = null): CompleteResult()
    class Error(val errorText: String): CompleteResult()

    fun printResult(printSuccess: Boolean = true){
        when (val res = this) {
            is Error -> println("[ERROR] ${res.errorText}")
            is Success -> if (printSuccess) println("[SUCCESS] ${res.successText}")
        }
    }
}

open class FireBaseData (
    var SYS_UUID: String = "",
    var SYS_FIRE_PATH: String? = null,
    var SYS_CREATE_DATE: Long = System.currentTimeMillis()
){
    @Exclude
    fun toCollection() : CollectionReference {
        val stringPath = SYS_FIRE_PATH!!.removeSuffix(SYS_UUID)
        println("PATH: $stringPath")
        return FirebaseService.firestore.collection(stringPath)
    }

    fun toDocument() : DocumentReference {
        return FirebaseService.firestore.document(SYS_FIRE_PATH!!)
    }

    @Exclude
    fun fireSaveData() : CompleteResult {
        if (SYS_FIRE_PATH == null) return CompleteResult.Error("Field with Firestore path is NULL")
        val db = FirebaseService.firestore
        val result = db.runTransaction {
            try {
                it.set(db.document(SYS_FIRE_PATH!!), this@FireBaseData)
                CompleteResult.Success()
            }catch (e: Exception) {
                CompleteResult.Error(e.message?:"")
            }
        }.get()
        return result
    }

    @Exclude
    fun deleteData() : CompleteResult {
        if (SYS_FIRE_PATH == null) return CompleteResult.Error("Field with Firestore path is NULL")
        val db = FirebaseService.firestore
        val result = db.runTransaction { transaction ->
            try {
                transaction.delete(db.document(SYS_FIRE_PATH!!))
                CompleteResult.Success("Delete successfully completed")
            }catch (e: Exception) {
                CompleteResult.Error(e.message?:"")
            }
        }.get()
        return result
    }
}