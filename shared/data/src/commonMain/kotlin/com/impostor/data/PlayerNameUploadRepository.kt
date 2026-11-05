package com.impostor.data

import com.impostor.domain.PlayerNameUploadRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.Serializable

@Serializable
internal data class PlayerNameUploadDocument(
    val names: List<String>,
    val createdAt: FieldValue,
)

internal interface PlayerNameUploadDataSource {
    suspend fun create(names: List<String>)
}

internal class FirebasePlayerNameUploadDataSource(
    private val auth: FirebaseAuth = Firebase.auth,
    private val firestore: FirebaseFirestore = Firebase.firestore,
) : PlayerNameUploadDataSource {
    override suspend fun create(names: List<String>) {
        if (auth.currentUser == null) auth.signInAnonymously()
        firestore.collection("playerNameUploads").add(
            PlayerNameUploadDocument(
                names = names,
                createdAt = FieldValue.serverTimestamp,
            ),
        )
    }
}

internal class FirebasePlayerNameUploadRepository(
    private val dataSource: PlayerNameUploadDataSource,
) : PlayerNameUploadRepository {
    override suspend fun uploadNames(names: List<String>) {
        dataSource.create(names)
    }
}

fun playerNameUploadRepository(): PlayerNameUploadRepository =
    FirebasePlayerNameUploadRepository(FirebasePlayerNameUploadDataSource())
