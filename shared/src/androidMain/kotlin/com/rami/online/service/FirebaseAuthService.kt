package com.rami.online.service

import com.rami.online.model.OnlinePlayer
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseAuthService : AuthService {

    private val auth get() = Firebase.auth
    private val db   get() = Firebase.firestore

    override val isSignedIn: Boolean get() = auth.currentUser != null
    override val uid: String?        get() = auth.currentUser?.uid

    override val currentPlayer: Flow<OnlinePlayer?> =
        auth.authStateChanged.map { user ->
            user?.uid?.let { fetchPlayer(it) }
        }

    override suspend fun signInWithEmail(email: String, password: String): Result<OnlinePlayer> =
        runCatching {
            val result = auth.signInWithEmailAndPassword(email, password)
            fetchOrCreatePlayer(result.user!!.uid, result.user!!.displayName ?: "Player")
        }

    override suspend fun signUpWithEmail(email: String, password: String, username: String): Result<OnlinePlayer> =
        runCatching {
            val result = auth.createUserWithEmailAndPassword(email, password)
            val uid = result.user!!.uid
            val player = OnlinePlayer(uid = uid, username = username)
            db.collection("players").document(uid).set(player)
            player
        }

    override suspend fun signInWithGoogle(idToken: String): Result<OnlinePlayer> =
        runCatching {
            val credential = GoogleAuthProvider.credential(idToken, null)
            val result = auth.signInWithCredential(credential)
            fetchOrCreatePlayer(result.user!!.uid, result.user!!.displayName ?: "Player")
        }

    override suspend fun signInAsGuest(): Result<OnlinePlayer> =
        runCatching {
            val result = auth.signInAnonymously()
            val uid = result.user!!.uid
            val player = OnlinePlayer(uid = uid, username = "Guest_${uid.take(6)}")
            db.collection("players").document(uid).set(player)
            player
        }

    override suspend fun signOut() = auth.signOut()

    override suspend fun updateUsername(username: String): Result<Unit> = runCatching {
        val uid = uid ?: error("Not signed in")
        db.collection("players").document(uid).update("username" to username)
    }

    override suspend fun updateAvatar(avatarId: Int): Result<Unit> = runCatching {
        val uid = uid ?: error("Not signed in")
        db.collection("players").document(uid).update("avatarId" to avatarId)
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        val uid = uid ?: error("Not signed in")
        db.collection("players").document(uid).delete()
        auth.currentUser!!.delete()
    }

    private suspend fun fetchPlayer(uid: String): OnlinePlayer? =
        db.collection("players").document(uid).get().data<OnlinePlayer?>()

    private suspend fun fetchOrCreatePlayer(uid: String, displayName: String): OnlinePlayer {
        val existing = fetchPlayer(uid)
        if (existing != null) return existing
        val player = OnlinePlayer(uid = uid, username = displayName)
        db.collection("players").document(uid).set(player)
        return player
    }
}
