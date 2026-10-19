package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.ChatMessageEntity
import com.example.data.model.MomentEntity
import com.example.data.model.UserProfileEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    }
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

class FirebaseManager(
    private val firestore: FirebaseFirestore?,
    val auth: FirebaseAuth?
) {
    constructor(context: Context) : this(
        firestore = try {
            if (FirebaseApp.getApps(context.applicationContext).isNotEmpty()) {
                FirebaseFirestore.getInstance(
                    context.applicationContext.getString(R.string.firestore_database_id)
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("FirebaseManager", "Firestore init notice: ${e.message}")
            null
        },
        auth = try {
            if (FirebaseApp.getApps(context.applicationContext).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("FirebaseManager", "Auth init notice: ${e.message}")
            null
        }
    )

    private val _isCloudActive = MutableStateFlow(firestore != null)
    val isCloudActive = _isCloudActive.asStateFlow()

    fun isConfigured(): Boolean = _isCloudActive.value && firestore != null

    fun getCurrentUid(): String? = auth?.currentUser?.uid

    fun requireUserId(): String {
        return auth?.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // Sign in to Firebase Auth using Google ID Token from CredentialManager
    suspend fun signInWithGoogleCredential(idToken: String): Result<String> {
        val firebaseAuth = auth
            ?: return Result.failure(IllegalStateException("Firebase Auth is not initialized"))
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val uid = result.user?.uid ?: error("Authenticated Google user UID is null")
            Result.success(uid)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Google credential sign-in failed", e)
            Result.failure(e)
        }
    }

    // Fetch existing UserProfile from Cloud Firestore
    suspend fun fetchUserProfile(userId: String): UserProfileEntity? {
        val db = firestore ?: return null
        val publicPath = "public_profiles/$userId"
        return try {
            val doc = db.collection("public_profiles").document(userId).get().await()
            if (!doc.exists()) return null

            val email = if (auth?.currentUser?.uid == userId) {
                try {
                    val privateDoc = db.collection("users").document(userId).get().await()
                    privateDoc.getString("email") ?: auth.currentUser?.email.orEmpty()
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.GET, "users/$userId")
                    auth.currentUser?.email.orEmpty()
                }
            } else {
                ""
            }

            docToUserProfileEntity(doc, email = email, isCurrentUser = (auth?.currentUser?.uid == userId))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, publicPath)
            throw e
        }
    }

    fun getConversationChannelId(user1: String, user2: String): String {
        return listOf(user1, user2).sorted().joinToString("_")
    }

    // 1. Sync User Profile to Cloud Firestore (Split Private PII & Public Profile)
    suspend fun saveUserProfileToCloud(user: UserProfileEntity): Result<String> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore not initialized"))
        val uid = requireUserId()
        val privateRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)

        return try {
            val existingPrivate = privateRef.get().await()
            if (!existingPrivate.exists()) {
                val privateCreate = mapOf(
                    "userId" to uid,
                    "name" to user.name.ifBlank { "Üye" }.take(100),
                    "email" to user.email.take(254),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                privateRef.set(privateCreate).await()
            } else {
                val privateUpdate = mapOf(
                    "name" to user.name.ifBlank { "Üye" }.take(100),
                    "email" to user.email.take(254),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                privateRef.update(privateUpdate).await()
            }

            val existingPublic = publicRef.get().await()
            val basePublicFields = mapOf(
                "name" to user.name.ifBlank { "Üye" }.take(100),
                "age" to user.age.coerceIn(18, 120),
                "city" to user.city.take(100),
                "bio" to user.bio.take(1000),
                "occupation" to user.occupation.take(120),
                "education" to user.education.take(120),
                "heightCm" to user.heightCm.coerceIn(100, 250),
                "zodiac" to user.zodiac.take(50),
                "lookingFor" to user.lookingFor.take(100),
                "photosJson" to user.photosJson.take(4000),
                "interestsJson" to user.interestsJson.take(2000),
                "lifestyleJson" to user.lifestyleJson.take(2000),
                "promptQuestion" to user.promptQuestion.take(300),
                "promptAnswer" to user.promptAnswer.take(1000),
                "isVerified" to user.isVerified,
                "isVip" to user.isVip,
                "latitude" to user.latitude,
                "longitude" to user.longitude,
                "isOnline" to true,
                "visibility" to "public",
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (!existingPublic.exists()) {
                val createPayload = basePublicFields + mapOf(
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                publicRef.set(createPayload).await()
            } else {
                publicRef.update(basePublicFields).await()
            }
            Result.success(uid)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, publicRef.path)
            Result.failure(e)
        }
    }

    fun syncUserProfile(user: UserProfileEntity) {
        val db = firestore ?: return
        val uid = auth?.currentUser?.uid ?: return
        val publicRef = db.collection("public_profiles").document(uid)
        publicRef.get()
            .addOnSuccessListener { existing ->
                val basePublicFields = mapOf(
                    "name" to user.name.ifBlank { "Üye" }.take(100),
                    "age" to user.age.coerceIn(18, 120),
                    "city" to user.city.take(100),
                    "bio" to user.bio.take(1000),
                    "occupation" to user.occupation.take(120),
                    "education" to user.education.take(120),
                    "heightCm" to user.heightCm.coerceIn(100, 250),
                    "zodiac" to user.zodiac.take(50),
                    "lookingFor" to user.lookingFor.take(100),
                    "photosJson" to user.photosJson.take(4000),
                    "interestsJson" to user.interestsJson.take(2000),
                    "lifestyleJson" to user.lifestyleJson.take(2000),
                    "promptQuestion" to user.promptQuestion.take(300),
                    "promptAnswer" to user.promptAnswer.take(1000),
                    "isVerified" to user.isVerified,
                    "isVip" to user.isVip,
                    "latitude" to user.latitude,
                    "longitude" to user.longitude,
                    "isOnline" to true,
                    "visibility" to "public",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                val task = if (!existing.exists()) {
                    publicRef.set(
                        basePublicFields + mapOf(
                            "userId" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    publicRef.update(basePublicFields)
                }
                task.addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.WRITE, publicRef.path)
                }
            }
            .addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.GET, publicRef.path)
            }
    }

    // 2. Real-time Listen to Other Public Profiles from Cloud Firestore
    fun listenToCloudUsers(currentUserId: String): Flow<List<UserProfileEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val path = "public_profiles"
        val listener = db.collection(path)
            .whereEqualTo("visibility", "public")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val users = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("userId") ?: doc.id
                        if (id == currentUserId || id == "current_user_me" ||
                            id.startsWith("user_elif") || id.startsWith("user_deniz") ||
                            id.startsWith("user_zeynep") || id.startsWith("user_selin") ||
                            id.startsWith("user_mert") || id.startsWith("user_can") ||
                            id.startsWith("user_aylin") || id.startsWith("demo_") ||
                            id.startsWith("seed_")
                        ) {
                            return@mapNotNull null
                        }
                        docToUserProfileEntity(doc, email = "", isCurrentUser = false)
                    }
                    trySend(users)
                }
            }

        awaitClose { listener.remove() }
    }

    // 3. Send Message to Cloud Firestore
    fun sendCloudMessage(channelId: String, senderId: String, partnerId: String, message: ChatMessageEntity) {
        val db = firestore ?: return
        val uid = auth?.currentUser?.uid ?: senderId
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val msgRef = db.collection("conversations")
            .document(channelId)
            .collection("messages")
            .document(msgId)

        val rawMap = mapOf<String, Any?>(
            "channelId" to channelId,
            "senderId" to uid,
            "participantIds" to listOf(uid, partnerId).sorted(),
            "text" to message.text.take(2000),
            "mediaUri" to message.mediaUri,
            "mediaType" to message.mediaType,
            "createdAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        msgRef.set(rawMap)
            .addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            }
    }

    // 4. Real-time Listen to Chat Messages in a Conversation
    fun listenToCloudMessages(channelId: String, myUserId: String, partnerId: String): Flow<List<ChatMessageEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val uid = auth?.currentUser?.uid ?: myUserId
        val path = "conversations/$channelId/messages"
        val listener = db.collection("conversations")
            .document(channelId)
            .collection("messages")
            .whereArrayContains("participantIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.map { doc ->
                        val remoteSenderId = doc.getString("senderId") ?: "unknown"
                        val createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        val timestamp = createdAt?.toDate()?.time ?: System.currentTimeMillis()
                        val stableId = (doc.id.hashCode().toLong() and 0x7FFFFFFF) + 1000L

                        ChatMessageEntity(
                            id = stableId,
                            conversationId = partnerId,
                            senderId = if (remoteSenderId == uid || remoteSenderId == "me") "me" else partnerId,
                            text = doc.getString("text") ?: "",
                            mediaUri = doc.getString("mediaUri"),
                            mediaType = doc.getString("mediaType"),
                            timestamp = timestamp
                        )
                    }.sortedBy { it.timestamp }
                    trySend(messages)
                }
            }

        awaitClose { listener.remove() }
    }

    // 5. Publish Moment / Story to Cloud Firestore
    suspend fun createCloudMoment(moment: MomentEntity): Result<String> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore not initialized"))
        val uid = requireUserId()
        val momentId = moment.id.ifBlank { "moment_${UUID.randomUUID().toString().replace("-", "").take(20)}" }
        val docRef = db.collection("moments").document(momentId)
        val momentMap = mapOf(
            "id" to momentId,
            "authorId" to uid,
            "authorName" to moment.authorName.ifBlank { "Üye" }.take(100),
            "authorAvatar" to moment.authorAvatar.take(2000),
            "mediaUri" to moment.mediaUri.take(2000),
            "mediaType" to if (moment.mediaType == "VIDEO") "VIDEO" else "IMAGE",
            "caption" to moment.caption.take(1000),
            "location" to moment.location.take(200),
            "likesCount" to moment.likesCount.coerceAtLeast(0),
            "visibility" to "public",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(momentMap).await()
            Result.success(momentId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun publishCloudMoment(moment: MomentEntity) {
        val db = firestore ?: return
        val uid = auth?.currentUser?.uid ?: return
        val momentId = moment.id.ifBlank { "moment_${UUID.randomUUID().toString().replace("-", "").take(20)}" }
        val docRef = db.collection("moments").document(momentId)
        val momentMap = mapOf(
            "id" to momentId,
            "authorId" to uid,
            "authorName" to moment.authorName.ifBlank { "Üye" }.take(100),
            "authorAvatar" to moment.authorAvatar.take(2000),
            "mediaUri" to moment.mediaUri.take(2000),
            "mediaType" to if (moment.mediaType == "VIDEO") "VIDEO" else "IMAGE",
            "caption" to moment.caption.take(1000),
            "location" to moment.location.take(200),
            "likesCount" to moment.likesCount.coerceAtLeast(0),
            "visibility" to "public",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        docRef.set(momentMap)
            .addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
            }
    }

    // 6. Update Moment Like Count on Cloud Firestore
    fun updateMomentLikeCount(momentId: String, newCount: Int) {
        val db = firestore ?: return
        val docRef = db.collection("moments").document(momentId)
        docRef.update(
            mapOf(
                "likesCount" to newCount.coerceAtLeast(0),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).addOnFailureListener { e ->
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    // 7. Real-time Listen to Moments
    fun listenToCloudMoments(): Flow<List<MomentEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val path = "moments"
        val listener = db.collection(path)
            .whereEqualTo("visibility", "public")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val moments = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        if (id.startsWith("moment_1") || id.startsWith("moment_2") ||
                            id.startsWith("moment_3") || id.startsWith("moment_4") ||
                            id.startsWith("moment_seed")
                        ) {
                            return@mapNotNull null
                        }
                        val createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        val timestamp = createdAt?.toDate()?.time ?: System.currentTimeMillis()

                        MomentEntity(
                            id = id,
                            authorId = doc.getString("authorId") ?: "",
                            authorName = doc.getString("authorName") ?: "",
                            authorAvatar = doc.getString("authorAvatar") ?: "",
                            mediaUri = doc.getString("mediaUri") ?: "",
                            mediaType = doc.getString("mediaType") ?: "IMAGE",
                            caption = doc.getString("caption") ?: "",
                            location = doc.getString("location") ?: "",
                            likesCount = (doc.getLong("likesCount") ?: 0L).toInt(),
                            isLiked = false,
                            timestamp = timestamp
                        )
                    }.sortedByDescending { it.timestamp }
                    trySend(moments)
                }
            }

        awaitClose { listener.remove() }
    }

    // 8. Record Cloud Like and check for mutual match
    fun recordCloudLike(myUserId: String, partnerId: String, isSuperLike: Boolean, onMutualMatch: () -> Unit) {
        val db = firestore ?: return
        val uid = auth?.currentUser?.uid ?: myUserId
        if (uid == partnerId) return
        val likeDocId = "${uid}_likes_${partnerId}"
        val likeRef = db.collection("likes").document(likeDocId)

        likeRef.get()
            .addOnSuccessListener { existing ->
                val task = if (!existing.exists()) {
                    likeRef.set(
                        mapOf(
                            "fromUid" to uid,
                            "toUid" to partnerId,
                            "isSuperLike" to isSuperLike,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    likeRef.update(
                        mapOf(
                            "isSuperLike" to isSuperLike,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                task.addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.WRITE, likeRef.path)
                }
            }
            .addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.GET, likeRef.path)
            }

        db.collection("likes")
            .whereEqualTo("fromUid", partnerId)
            .whereEqualTo("toUid", uid)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    onMutualMatch()
                }
            }
            .addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.LIST, "likes")
            }
    }

    private fun docToUserProfileEntity(
        doc: DocumentSnapshot,
        email: String,
        isCurrentUser: Boolean
    ): UserProfileEntity {
        val id = doc.getString("userId") ?: doc.id
        return UserProfileEntity(
            id = id,
            name = doc.getString("name") ?: "Üye",
            age = (doc.getLong("age") ?: 24L).toInt(),
            city = doc.getString("city") ?: "İstanbul",
            bio = doc.getString("bio") ?: "",
            occupation = doc.getString("occupation") ?: "",
            education = doc.getString("education") ?: "",
            heightCm = (doc.getLong("heightCm") ?: 175L).toInt(),
            zodiac = doc.getString("zodiac") ?: "Aslan ♌",
            lookingFor = doc.getString("lookingFor") ?: "Ciddi İlişki ❤️",
            photosJson = doc.getString("photosJson") ?: "",
            interestsJson = doc.getString("interestsJson") ?: "",
            lifestyleJson = doc.getString("lifestyleJson") ?: "",
            promptQuestion = doc.getString("promptQuestion") ?: "",
            promptAnswer = doc.getString("promptAnswer") ?: "",
            isVerified = doc.getBoolean("isVerified") ?: true,
            latitude = doc.getDouble("latitude") ?: 41.0082,
            longitude = doc.getDouble("longitude") ?: 28.9784,
            distanceKm = if (isCurrentUser) 0.0 else 2.4,
            isOnline = doc.getBoolean("isOnline") ?: true,
            isVip = doc.getBoolean("isVip") ?: false,
            email = email,
            isCurrentUser = isCurrentUser,
            isProfileSetupCompleted = true
        )
    }
}
