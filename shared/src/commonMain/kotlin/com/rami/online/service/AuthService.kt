package com.rami.online.service

import com.rami.online.model.OnlinePlayer
import kotlinx.coroutines.flow.Flow

interface AuthService {
    val currentPlayer: Flow<OnlinePlayer?>
    val isSignedIn: Boolean
    val uid: String?

    suspend fun signInWithEmail(email: String, password: String): Result<OnlinePlayer>
    suspend fun signUpWithEmail(email: String, password: String, username: String): Result<OnlinePlayer>
    suspend fun signInWithGoogle(idToken: String): Result<OnlinePlayer>
    suspend fun signInAsGuest(): Result<OnlinePlayer>
    suspend fun signOut()
    suspend fun updateUsername(username: String): Result<Unit>
    suspend fun updateAvatar(avatarId: Int): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
}
