package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val age: Int,
    val city: String,
    val bio: String,
    val occupation: String,
    val education: String,
    val heightCm: Int,
    val zodiac: String,
    val lookingFor: String,
    val photosJson: String, // comma separated URLs / local URIs
    val interestsJson: String, // comma separated tags
    val lifestyleJson: String, // comma separated lifestyle tags
    val promptQuestion: String,
    val promptAnswer: String,
    val isVerified: Boolean = true,
    val latitude: Double = 41.0082,
    val longitude: Double = 28.9784,
    val distanceKm: Double = 1.2,
    val isOnline: Boolean = true,
    val isMatch: Boolean = false,
    val isLikedByMe: Boolean = false,
    val isSuperLiked: Boolean = false,
    val isBlocked: Boolean = false,
    val isReported: Boolean = false,
    val isCurrentUser: Boolean = false,
    val isVip: Boolean = false,
    val email: String = "",
    val isProfileSetupCompleted: Boolean = false
) {
    val photoList: List<String>
        get() = if (photosJson.isBlank()) emptyList() else photosJson.split("|||")

    val interestList: List<String>
        get() = if (interestsJson.isBlank()) emptyList() else interestsJson.split("|||")

    val lifestyleList: List<String>
        get() = if (lifestyleJson.isBlank()) emptyList() else lifestyleJson.split("|||")
}

@Entity(tableName = "messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val senderId: String, // "me" or user id
    val text: String,
    val mediaUri: String? = null,
    val mediaType: String? = null, // "IMAGE", "VIDEO"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = true
)

@Entity(tableName = "moments")
data class MomentEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String,
    val mediaUri: String,
    val mediaType: String, // "IMAGE" or "VIDEO"
    val caption: String,
    val location: String,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
