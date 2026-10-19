package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.MomentEntity
import com.example.data.model.UserProfileEntity
import com.example.location.GoogleLocationTracker
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class DatingRepository(
    private val context: Context,
    private val db: AppDatabase = AppDatabase.getInstance(context),
    val firebaseManager: FirebaseManager = FirebaseManager(context)
) {
    private val userDao = db.userDao()
    private val messageDao = db.messageDao()
    private val momentDao = db.momentDao()

    private val authPrefs = context.applicationContext.getSharedPreferences(
        "kalpkalbe_email_auth",
        Context.MODE_PRIVATE
    )

    private val repoScope = CoroutineScope(Dispatchers.IO)
    private var usersSyncJob: Job? = null
    private var momentsSyncJob: Job? = null

    private val _matchCelebrationEvent = MutableSharedFlow<UserProfileEntity>(extraBufferCapacity = 1)
    val matchCelebrationEvent = _matchCelebrationEvent.asSharedFlow()

    init {
        repoScope.launch {
            cleanupLegacyFakeData()
            // Auth-gate Firestore listeners: only attach listeners when a user is signed in with Firebase
            if (firebaseManager.getCurrentUid() != null) {
                startAuthenticatedCloudSync()
            }
        }
    }

    fun getActiveEmailUid(): String? = authPrefs.getString("active_email_uid", null)

    fun hasActiveSession(): Boolean {
        return firebaseManager.getCurrentUid() != null || !getActiveEmailUid().isNullOrBlank()
    }

    private fun hashCredential(email: String, password: String): String {
        val input = "${email.trim().lowercase()}::kalpkalbe_salt::$password"
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun deterministicEmailUid(email: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(email.trim().lowercase().toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }.take(20)
        return "email_$hex"
    }

    suspend fun registerWithEmailAccount(
        name: String,
        email: String,
        password: String
    ): Result<UserProfileEntity> {
        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase()

        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Lütfen geçerli bir ad soyad girin (en az 2 karakter)."))
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Lütfen geçerli bir e-posta adresi girin."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Şifreniz en az 6 karakter olmalıdır."))
        }

        val existingHash = authPrefs.getString("pwd_$cleanEmail", null)
        val existingDbUser = userDao.getUserByEmailDirect(cleanEmail)
        if (existingHash != null || (existingDbUser != null && existingDbUser.isProfileSetupCompleted)) {
            return Result.failure(
                IllegalArgumentException("Bu e-posta adresi zaten kayıtlı. Lütfen Giriş Yap sekmesini kullanın.")
            )
        }

        val uid = existingDbUser?.id ?: deterministicEmailUid(cleanEmail)
        val pwdHash = hashCredential(cleanEmail, password)

        authPrefs.edit()
            .putString("pwd_$cleanEmail", pwdHash)
            .putString("name_$cleanEmail", cleanName)
            .putString("uid_$cleanEmail", uid)
            .putString("active_email_uid", uid)
            .apply()

        userDao.clearCurrentUserFlags()

        val newProfile = UserProfileEntity(
            id = uid,
            name = cleanName,
            age = 24,
            city = "İstanbul",
            bio = "",
            occupation = "",
            education = "",
            heightCm = 170,
            zodiac = "Koç ♈",
            lookingFor = "Ciddi İlişki ❤️",
            photosJson = "",
            interestsJson = "Kahve ☕|||Müzik 🎵|||Seyahat ✈️",
            lifestyleJson = "Sosyal|||Pozitif",
            promptQuestion = "İdeal ilk buluşma",
            promptAnswer = "Güzel bir kahve ve samimi sohbet",
            isVerified = false,
            isCurrentUser = true,
            isVip = false,
            email = cleanEmail,
            isProfileSetupCompleted = false
        )

        userDao.insertUser(newProfile)
        return Result.success(newProfile)
    }

    suspend fun loginWithEmailAccount(
        email: String,
        password: String
    ): Result<UserProfileEntity> {
        val cleanEmail = email.trim().lowercase()
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Lütfen geçerli bir e-posta adresi girin."))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Lütfen şifrenizi girin."))
        }

        val storedHash = authPrefs.getString("pwd_$cleanEmail", null)
        val existingDbUser = userDao.getUserByEmailDirect(cleanEmail)

        if (storedHash == null && existingDbUser == null) {
            return Result.failure(
                IllegalArgumentException("Bu e-posta ile kayıtlı hesap bulunamadı. Lütfen önce Kayıt Ol sekmesinden kayıt olun.")
            )
        }

        val providedHash = hashCredential(cleanEmail, password)
        if (storedHash != null && storedHash != providedHash) {
            return Result.failure(IllegalArgumentException("E-posta veya şifre hatalı. Lütfen tekrar deneyin."))
        }

        // If user existed in Room DB before password hash was saved, persist hash now
        val uid = existingDbUser?.id
            ?: authPrefs.getString("uid_$cleanEmail", null)
            ?: deterministicEmailUid(cleanEmail)

        if (storedHash == null) {
            authPrefs.edit()
                .putString("pwd_$cleanEmail", providedHash)
                .putString("uid_$cleanEmail", uid)
                .apply()
        }

        authPrefs.edit()
            .putString("active_email_uid", uid)
            .apply()

        userDao.clearCurrentUserFlags()

        val activeProfile = if (existingDbUser != null) {
            val updated = existingDbUser.copy(isCurrentUser = true)
            userDao.insertUser(updated)
            updated
        } else {
            val savedName = authPrefs.getString("name_$cleanEmail", "Üye") ?: "Üye"
            val created = UserProfileEntity(
                id = uid,
                name = savedName,
                age = 24,
                city = "İstanbul",
                bio = "",
                occupation = "",
                education = "",
                heightCm = 170,
                zodiac = "Koç ♈",
                lookingFor = "Ciddi İlişki ❤️",
                photosJson = "",
                interestsJson = "Kahve ☕|||Müzik 🎵|||Seyahat ✈️",
                lifestyleJson = "Sosyal|||Pozitif",
                promptQuestion = "İdeal ilk buluşma",
                promptAnswer = "Güzel bir kahve ve samimi sohbet",
                isVerified = false,
                isCurrentUser = true,
                isVip = false,
                email = cleanEmail,
                isProfileSetupCompleted = false
            )
            userDao.insertUser(created)
            created
        }

        return Result.success(activeProfile)
    }

    suspend fun clearLocalAuthSession() {
        authPrefs.edit().remove("active_email_uid").apply()
        userDao.clearCurrentUserFlags()
    }

    fun startAuthenticatedCloudSync() {
        val currentUid = firebaseManager.getCurrentUid() ?: return
        if (!firebaseManager.isConfigured()) return

        if (usersSyncJob?.isActive != true) {
            usersSyncJob = repoScope.launch {
                firebaseManager.listenToCloudUsers(currentUid)
                    .catch { e -> Log.w("DatingRepository", "Users stream error: ${e.message}") }
                    .collect { onlineUsers ->
                        userDao.deleteFakeProfiles()
                        if (onlineUsers.isNotEmpty()) {
                            val me = userDao.getCurrentUserDirect()
                            val myLat = me?.latitude ?: 41.0082
                            val myLng = me?.longitude ?: 28.9784
                            val existingMap = userDao.getAllOtherProfilesDirect().associateBy { it.id }

                            val enrichedUsers = onlineUsers.map { cloudUser ->
                                val prev = existingMap[cloudUser.id]
                                val distKm = GoogleLocationTracker.calculateDistanceKm(
                                    myLat,
                                    myLng,
                                    cloudUser.latitude,
                                    cloudUser.longitude
                                )
                                cloudUser.copy(
                                    distanceKm = distKm,
                                    isMatch = prev?.isMatch ?: cloudUser.isMatch,
                                    isLikedByMe = prev?.isLikedByMe ?: cloudUser.isLikedByMe,
                                    isSuperLiked = prev?.isSuperLiked ?: cloudUser.isSuperLiked,
                                    isBlocked = prev?.isBlocked ?: false,
                                    isReported = prev?.isReported ?: false
                                )
                            }
                            userDao.insertAll(enrichedUsers)
                        }
                    }
            }
        }

        if (momentsSyncJob?.isActive != true) {
            momentsSyncJob = repoScope.launch {
                firebaseManager.listenToCloudMoments()
                    .catch { e -> Log.w("DatingRepository", "Moments stream error: ${e.message}") }
                    .collect { onlineMoments ->
                        momentDao.deleteFakeMoments()
                        if (onlineMoments.isNotEmpty()) {
                            momentDao.insertAll(onlineMoments)
                        }
                    }
            }
        }
    }

    fun stopCloudSync() {
        usersSyncJob?.cancel()
        usersSyncJob = null
        momentsSyncJob?.cancel()
        momentsSyncJob = null
    }

    private suspend fun cleanupLegacyFakeData() {
        userDao.deleteFakeProfiles()
        momentDao.deleteFakeMoments()
        messageDao.deleteFakeMessages()

        val existingCurrent = userDao.getCurrentUserDirect()
        if (existingCurrent != null && existingCurrent.name == "Kaan" && existingCurrent.email.isBlank()) {
            userDao.deleteAllOtherUsers()
        }
    }

    fun getAllProfiles(): Flow<List<UserProfileEntity>> = userDao.getAllOtherProfiles()
    fun getCurrentUser(): Flow<UserProfileEntity?> = userDao.getCurrentUser()
    fun getMatches(): Flow<List<UserProfileEntity>> = userDao.getMatches()
    fun getUserById(id: String): Flow<UserProfileEntity?> = userDao.getUserById(id)

    fun getMessagesForConversation(convId: String): Flow<List<ChatMessageEntity>> {
        val myUid = firebaseManager.getCurrentUid()
        if (firebaseManager.isConfigured() && myUid != null) {
            repoScope.launch {
                val channelId = firebaseManager.getConversationChannelId(myUid, convId)
                firebaseManager.listenToCloudMessages(channelId, myUid, convId)
                    .catch { e -> Log.w("DatingRepository", "Messages stream error: ${e.message}") }
                    .collect { cloudMsgs ->
                        if (cloudMsgs.isNotEmpty()) {
                            messageDao.insertAll(cloudMsgs)
                        }
                    }
            }
        }
        return messageDao.getMessagesForConversation(convId)
    }

    fun getAllMoments(): Flow<List<MomentEntity>> = momentDao.getAllMoments()

    // Direct Cloud Firestore operations (used by both production sync and Robolectric RuleTests)
    suspend fun saveUserProfileToCloud(profile: UserProfileEntity): Result<String> {
        return firebaseManager.saveUserProfileToCloud(profile)
    }

    suspend fun fetchUserProfileFromCloud(userId: String): UserProfileEntity? {
        return firebaseManager.fetchUserProfile(userId)
    }

    fun observePublicProfilesFromCloud(currentUserId: String): Flow<List<UserProfileEntity>> {
        return firebaseManager.listenToCloudUsers(currentUserId)
    }

    suspend fun createMomentInCloud(moment: MomentEntity): Result<String> {
        return firebaseManager.createCloudMoment(moment)
    }

    fun observeMomentsFromCloud(): Flow<List<MomentEntity>> {
        return firebaseManager.listenToCloudMoments()
    }

    suspend fun likeProfile(profile: UserProfileEntity, isSuperLike: Boolean = false): Boolean {
        val currentUser = userDao.getCurrentUserDirect()
        val myId = firebaseManager.getCurrentUid() ?: currentUser?.id ?: "me"

        val isMutualMatch = isSuperLike || profile.isMatch
        userDao.setLiked(profile.id, isMatch = isMutualMatch, isSuperLike = isSuperLike)

        if (isMutualMatch) {
            val updatedProfile = profile.copy(isMatch = true, isLikedByMe = true, isSuperLiked = isSuperLike)
            _matchCelebrationEvent.tryEmit(updatedProfile)
        }

        if (firebaseManager.isConfigured() && firebaseManager.getCurrentUid() != null) {
            firebaseManager.recordCloudLike(myId, profile.id, isSuperLike) {
                repoScope.launch {
                    userDao.setLiked(profile.id, isMatch = true, isSuperLike = isSuperLike)
                    _matchCelebrationEvent.tryEmit(profile.copy(isMatch = true, isLikedByMe = true))
                }
            }
        }

        return isMutualMatch
    }

    suspend fun passProfile(profile: UserProfileEntity) {
        // Passed without mock side-effects
    }

    suspend fun blockUser(userId: String) {
        userDao.blockUser(userId)
    }

    suspend fun reportUser(userId: String, reason: String) {
        userDao.reportUser(userId)
    }

    suspend fun verifyCurrentUser() {
        userDao.verifyCurrentUser()
    }

    suspend fun updateCurrentUser(updated: UserProfileEntity) {
        userDao.updateUser(updated)
        if (firebaseManager.getCurrentUid() != null) {
            firebaseManager.saveUserProfileToCloud(updated)
        }
    }

    suspend fun updateVipStatus(isVip: Boolean) {
        userDao.updateVipStatus(isVip)
        val user = userDao.getCurrentUserDirect()
        if (user != null && firebaseManager.getCurrentUid() != null) {
            firebaseManager.saveUserProfileToCloud(user.copy(isVip = isVip))
        }
    }

    suspend fun signInWithGoogleCredential(idToken: String): Result<UserProfileEntity?> {
        val authResult = firebaseManager.signInWithGoogleCredential(idToken)
        return authResult.mapCatching { uid ->
            authPrefs.edit().remove("active_email_uid").apply()
            userDao.clearCurrentUserFlags()
            startAuthenticatedCloudSync()
            val cloudProfile = try {
                firebaseManager.fetchUserProfile(uid)
            } catch (e: Exception) {
                null
            }
            if (cloudProfile != null) {
                userDao.insertUser(cloudProfile.copy(isCurrentUser = true))
            }
            cloudProfile
        }
    }

    suspend fun saveUserProfile(profile: UserProfileEntity) {
        val currentUid = firebaseManager.getCurrentUid()
        val emailUid = getActiveEmailUid()
        val effectiveId = when {
            !currentUid.isNullOrBlank() -> currentUid
            !emailUid.isNullOrBlank() -> emailUid
            else -> profile.id
        }
        val userToSave = profile.copy(
            id = effectiveId,
            isCurrentUser = true,
            isProfileSetupCompleted = true
        )
        userDao.clearCurrentUserFlags()
        userDao.insertUser(userToSave)
        if (currentUid != null) {
            firebaseManager.saveUserProfileToCloud(userToSave)
            startAuthenticatedCloudSync()
        }
    }

    suspend fun sendMessage(conversationId: String, text: String, mediaUri: String? = null, mediaType: String? = null) {
        val currentUser = userDao.getCurrentUserDirect()
        val myId = firebaseManager.getCurrentUid() ?: currentUser?.id ?: "me"

        val msg = ChatMessageEntity(
            conversationId = conversationId,
            senderId = "me",
            text = text,
            mediaUri = mediaUri,
            mediaType = mediaType,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(msg)

        if (firebaseManager.isConfigured() && firebaseManager.getCurrentUid() != null) {
            val channelId = firebaseManager.getConversationChannelId(myId, conversationId)
            firebaseManager.sendCloudMessage(channelId, myId, conversationId, msg)
        }
    }

    suspend fun addMoment(author: UserProfileEntity, caption: String, mediaUri: String, mediaType: String, location: String) {
        val uid = firebaseManager.getCurrentUid() ?: author.id
        val moment = MomentEntity(
            id = "moment_${System.currentTimeMillis()}",
            authorId = uid,
            authorName = author.name,
            authorAvatar = author.photoList.firstOrNull() ?: "",
            mediaUri = mediaUri,
            mediaType = mediaType,
            caption = caption,
            location = location,
            likesCount = 0,
            isLiked = false,
            timestamp = System.currentTimeMillis()
        )
        momentDao.insertMoment(moment)
        if (firebaseManager.getCurrentUid() != null) {
            firebaseManager.createCloudMoment(moment)
        }
    }

    suspend fun toggleMomentLike(moment: MomentEntity) {
        val newLiked = !moment.isLiked
        val newCount = if (newLiked) moment.likesCount + 1 else (moment.likesCount - 1).coerceAtLeast(0)
        momentDao.updateLike(moment.id, newLiked, newCount)
        if (firebaseManager.isConfigured() && firebaseManager.getCurrentUid() != null) {
            firebaseManager.updateMomentLikeCount(moment.id, newCount)
        }
    }

    suspend fun updateLocationAndRecalculateDistances(userLat: Double, userLng: Double) {
        val currentUser = userDao.getCurrentUserDirect()
        if (currentUser != null) {
            val updated = currentUser.copy(latitude = userLat, longitude = userLng, distanceKm = 0.0)
            userDao.updateUser(updated)
            if (firebaseManager.getCurrentUid() != null) {
                firebaseManager.saveUserProfileToCloud(updated)
            }
        }

        val otherUsers = userDao.getAllOtherProfilesDirect()
        if (otherUsers.isNotEmpty()) {
            val recalculated = otherUsers.map { profile ->
                val distKm = GoogleLocationTracker.calculateDistanceKm(
                    userLat,
                    userLng,
                    profile.latitude,
                    profile.longitude
                )
                profile.copy(distanceKm = distKm)
            }
            userDao.insertAll(recalculated)
        }
    }
}
