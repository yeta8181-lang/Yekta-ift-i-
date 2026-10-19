package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.firebase.FirebaseManager
import com.example.data.model.MomentEntity
import com.example.data.model.UserProfileEntity
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class DatingRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun createRepository(): DatingRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = FirebaseManager(firestore, auth)
        return DatingRepository(context = context, firebaseManager = manager)
    }

    private fun buildSampleProfile(uid: String, name: String, email: String): UserProfileEntity {
        return UserProfileEntity(
            id = uid,
            name = name,
            age = 26,
            city = "İstanbul",
            bio = "Kahve ve fotoğraf tutkunu",
            occupation = "Tasarımcı",
            education = "Boğaziçi Üniversitesi",
            heightCm = 172,
            zodiac = "Terazi ♎",
            lookingFor = "Ciddi İlişki ❤️",
            photosJson = "https://example.com/photo1.jpg",
            interestsJson = "Kahve ☕|||Müzik 🎵",
            lifestyleJson = "Sigara İçmiyor|||Spor Yapıyor",
            promptQuestion = "İdeal ilk buluşma",
            promptAnswer = "Sahilde yürüyüş ve kahve",
            isVerified = true,
            isVip = false,
            email = email,
            isCurrentUser = true,
            isProfileSetupCompleted = true
        )
    }

    @Test
    fun saveAndFetchUserProfile_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = createRepository()
        val profile = buildSampleProfile(aliceUid, "Alice", ALICE_EMAIL)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserProfileToCloud(profile)
        }
        assertTrue(saveResult.isSuccess)

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.fetchUserProfileFromCloud(aliceUid)
        }
        assertNotNull(fetched)
        assertEquals("Alice", fetched?.name)
        assertEquals(ALICE_EMAIL, fetched?.email)
    }

    @Test
    fun privateUserRecord_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = createRepository()
        val profile = buildSampleProfile(aliceUid, "Alice", ALICE_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveUserProfileToCloud(profile).getOrThrow()
        }

        // Bob signs in and attempts to read Alice's private PII document (/users/{aliceUid})
        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).get().await()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED when reading another user's private PII")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun observePublicProfiles_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = createRepository()
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observePublicProfilesFromCloud("unauth_user").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated public_profiles query")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun createAndObserveMoment_authenticatedUser_emitsMoment() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = createRepository()
        val momentId = "moment_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val moment = MomentEntity(
            id = momentId,
            authorId = aliceUid,
            authorName = "Alice",
            authorAvatar = "https://example.com/avatar.jpg",
            mediaUri = "https://example.com/moment.jpg",
            mediaType = "IMAGE",
            caption = "Boğaz manzarası",
            location = "Bebek, İstanbul",
            likesCount = 0
        )

        val createRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createMomentInCloud(moment)
        }
        assertTrue(createRes.isSuccess)

        val emittedMoments = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMomentsFromCloud().first { list -> list.any { it.id == momentId } }
        }
        assertTrue(emittedMoments.any { it.id == momentId && it.caption == "Boğaz manzarası" })
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
