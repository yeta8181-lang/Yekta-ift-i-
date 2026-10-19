package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserProfileEntity
import com.example.ui.components.InterestChip
import com.example.ui.components.MediaViewerDialog
import com.example.ui.theme.CupidGold
import com.example.ui.theme.NightCard
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightSurface
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.VerifiedBlue
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

enum class SwipeDirection {
    LEFT,
    RIGHT,
    UP
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverScreen(
    profiles: List<UserProfileEntity>,
    currentUser: UserProfileEntity? = null,
    isVip: Boolean = false,
    onLike: (UserProfileEntity, isSuperLike: Boolean) -> Unit,
    onPass: (UserProfileEntity) -> Unit,
    onOpenSafetyCenter: () -> Unit,
    onOpenReportDialog: (UserProfileEntity) -> Unit,
    onOpenVipPaywall: () -> Unit = {},
    onPublishMoment: (caption: String, mediaUri: String, mediaType: String, location: String) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedFilterTag by remember { mutableStateOf<String?>(null) }
    var showShareDialog by remember { mutableStateOf(false) }
    val swipedHistory = remember { mutableStateListOf<String>() }

    // Available interest tags from profiles & currentUser for filtering the swipe deck
    val allAvailableInterests by remember(profiles, currentUser) {
        derivedStateOf {
            val myInterests = currentUser?.interestList.orEmpty()
            val others = profiles.flatMap { it.interestList }
            (myInterests + others).map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(12)
        }
    }

    // Filtered profiles based on selected interest/bio tag
    val filteredProfiles by remember(profiles, selectedFilterTag) {
        derivedStateOf {
            val tag = selectedFilterTag
            if (tag.isNullOrBlank()) {
                profiles
            } else {
                profiles.filter { profile ->
                    profile.interestList.any { it.equals(tag, ignoreCase = true) } ||
                        profile.bio.contains(tag, ignoreCase = true) ||
                        profile.lookingFor.contains(tag, ignoreCase = true)
                }
            }
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedPhotoIndex by remember { mutableIntStateOf(0) }
    var isBioExpanded by remember { mutableStateOf(false) }
    var previewPhotoUri by remember { mutableStateOf<String?>(null) }

    // Reset index if filter changes and currentIndex is out of bounds
    LaunchedEffect(selectedFilterTag) {
        currentIndex = 0
        selectedPhotoIndex = 0
        isBioExpanded = false
    }

    val currentProfile = filteredProfiles.getOrNull(currentIndex)
    val nextProfile = filteredProfiles.getOrNull(currentIndex + 1)

    // Animatable swipe offsets for top card
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    val horizontalThreshold = 260f
    val verticalSuperLikeThreshold = -240f

    // Reset card position whenever candidate changes
    LaunchedEffect(currentProfile?.id, currentIndex) {
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
        selectedPhotoIndex = 0
        isBioExpanded = false
    }

    fun performAnimatedSwipe(direction: SwipeDirection, targetProfile: UserProfileEntity) {
        scope.launch {
            when (direction) {
                SwipeDirection.RIGHT -> {
                    launch { offsetX.animateTo(1100f, tween(240)) }
                    offsetY.animateTo(80f, tween(240))
                    swipedHistory.add(targetProfile.id)
                    onLike(targetProfile, false)
                }
                SwipeDirection.LEFT -> {
                    launch { offsetX.animateTo(-1100f, tween(240)) }
                    offsetY.animateTo(80f, tween(240))
                    swipedHistory.add(targetProfile.id)
                    onPass(targetProfile)
                }
                SwipeDirection.UP -> {
                    launch { offsetX.animateTo(0f, tween(240)) }
                    offsetY.animateTo(-1100f, tween(240))
                    swipedHistory.add(targetProfile.id)
                    onLike(targetProfile, true)
                }
            }
            currentIndex++
            selectedPhotoIndex = 0
            isBioExpanded = false
            offsetX.snapTo(0f)
            offsetY.snapTo(0f)
        }
    }

    val dragProgress = (
        maxOf(
            abs(offsetX.value) / horizontalThreshold,
            if (offsetY.value < 0f) abs(offsetY.value) / abs(verticalSuperLikeThreshold) else 0f
        )
        ).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NightSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // Top Share in Discover (VIP Only) & Interest Filter Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 6.dp)
                    .testTag("interest_filter_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // VIP-only Share in Discover button
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isVip) RoseAccent else CupidGold,
                    modifier = Modifier
                        .clickable {
                            if (isVip) {
                                showShareDialog = true
                            } else {
                                onOpenVipPaywall()
                            }
                        }
                        .testTag("discover_share_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "Keşfette Paylaş",
                            tint = if (isVip) Color.White else Color.Black,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isVip) "Keşfette Paylaş" else "Keşfette Paylaş (VIP)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isVip) Color.White else Color.Black
                        )
                    }
                }

                if (allAvailableInterests.isNotEmpty()) {
                    FilterChip(
                        selected = selectedFilterTag == null,
                        onClick = { selectedFilterTag = null },
                        label = {
                            Text(
                                text = "Tümü (${profiles.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoseAccent,
                            selectedLabelColor = Color.White,
                            containerColor = NightCard,
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilterTag == null,
                            borderColor = NightCardBorder,
                            selectedBorderColor = RoseAccent
                        )
                    )

                    allAvailableInterests.forEach { tag ->
                        val isSelected = selectedFilterTag.equals(tag, ignoreCase = true)
                        val isSharedWithMe = currentUser?.interestList?.any {
                            it.equals(tag, ignoreCase = true)
                        } == true
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedFilterTag = if (isSelected) null else tag
                            },
                            leadingIcon = if (isSharedWithMe) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else CupidGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    text = tag,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected || isSharedWithMe) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoseAccent,
                                selectedLabelColor = Color.White,
                                containerColor = NightCard,
                                labelColor = Color(0xFFE2E8F0)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSharedWithMe) CupidGold.copy(alpha = 0.5f) else NightCardBorder,
                                selectedBorderColor = RoseAccent
                            )
                        )
                    }
                }
            }

            if (currentProfile == null) {
                // Empty state when all cards in deck have been swiped or filtered out
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = RoseAccent.copy(alpha = 0.15f),
                        modifier = Modifier.size(96.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = RoseAccent,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = if (selectedFilterTag != null) {
                            "\"$selectedFilterTag\" İlgisine Uygun Kişi Kalmadı!"
                        } else {
                            "Şimdilik Çevrende Yeni Kişi Kalmadı!"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Kartları tekrar gözden geçirebilir, ilgi alanı filtresini sıfırlayabilir veya Harita sekmesinden radar aramasını genişletebilirsin.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            selectedFilterTag = null
                            currentIndex = 0
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseAccent),
                        modifier = Modifier.testTag("reset_cards_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kartları Başa Al", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Swipe Deck Container (Background next card + Foreground interactive card)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Underlying Next Card in Stack for depth preview
                    if (nextProfile != null) {
                        val nextScale = 0.93f + (0.07f * dragProgress)
                        val nextAlpha = 0.55f + (0.45f * dragProgress)
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = NightCard),
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = nextScale
                                    scaleY = nextScale
                                    alpha = nextAlpha
                                }
                                .border(1.dp, NightCardBorder, RoundedCornerShape(24.dp))
                        ) {
                            val nextPhoto = nextProfile.photoList.firstOrNull().orEmpty()
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (nextPhoto.isNotBlank()) {
                                    AsyncImage(
                                        model = nextPhoto,
                                        contentDescription = nextProfile.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    ProfileAvatarFallback(name = nextProfile.name)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                                            )
                                        )
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "${nextProfile.name}, ${nextProfile.age}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    // Active Top Swipeable Card
                    val photos = currentProfile.photoList
                    val activePhoto = photos.getOrNull(selectedPhotoIndex) ?: photos.firstOrNull() ?: ""
                    val compatibilityScore = remember(currentProfile, currentUser) {
                        calculateCompatibilityPercent(currentUser, currentProfile)
                    }
                    val sharedInterests = remember(currentProfile, currentUser) {
                        val mySet = currentUser?.interestList.orEmpty().map { it.lowercase() }.toSet()
                        currentProfile.interestList.filter { it.lowercase() in mySet }
                    }

                    val likeAlpha = (offsetX.value / horizontalThreshold).coerceIn(0f, 1f)
                    val passAlpha = (-offsetX.value / horizontalThreshold).coerceIn(0f, 1f)
                    val superLikeAlpha = if (offsetY.value < 0f && abs(offsetY.value) > abs(offsetX.value)) {
                        (abs(offsetY.value) / abs(verticalSuperLikeThreshold)).coerceIn(0f, 1f)
                    } else 0f

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = NightCard),
                        modifier = Modifier
                            .fillMaxSize()
                            .offset {
                                IntOffset(
                                    offsetX.value.roundToInt(),
                                    offsetY.value.roundToInt()
                                )
                            }
                            .graphicsLayer {
                                rotationZ = (offsetX.value / 25f).coerceIn(-16f, 16f)
                            }
                            .pointerInput(currentProfile.id) {
                                detectDragGestures(
                                    onDragEnd = {
                                        when {
                                            offsetX.value > horizontalThreshold -> {
                                                performAnimatedSwipe(SwipeDirection.RIGHT, currentProfile)
                                            }
                                            offsetX.value < -horizontalThreshold -> {
                                                performAnimatedSwipe(SwipeDirection.LEFT, currentProfile)
                                            }
                                            offsetY.value < verticalSuperLikeThreshold &&
                                                abs(offsetY.value) > abs(offsetX.value) -> {
                                                performAnimatedSwipe(SwipeDirection.UP, currentProfile)
                                            }
                                            else -> {
                                                scope.launch {
                                                    launch {
                                                        offsetX.animateTo(
                                                            0f,
                                                            spring(
                                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                                stiffness = Spring.StiffnessLow
                                                            )
                                                        )
                                                    }
                                                    offsetY.animateTo(
                                                        0f,
                                                        spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessLow
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        scope.launch {
                                            launch { offsetX.animateTo(0f, spring()) }
                                            offsetY.animateTo(0f, spring())
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        scope.launch {
                                            offsetX.snapTo(offsetX.value + dragAmount.x)
                                            offsetY.snapTo(offsetY.value + dragAmount.y)
                                        }
                                    }
                                )
                            }
                            .border(
                                width = 1.5.dp,
                                color = when {
                                    likeAlpha > 0.25f -> OnlineGreen.copy(alpha = 0.8f)
                                    passAlpha > 0.25f -> Color(0xFFEF4444).copy(alpha = 0.8f)
                                    superLikeAlpha > 0.25f -> CupidGold.copy(alpha = 0.85f)
                                    else -> NightCardBorder
                                },
                                shape = RoundedCornerShape(24.dp)
                            )
                            .testTag("swipe_card")
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Background Photo or Stylish Blank Placeholder
                            if (activePhoto.isNotBlank()) {
                                AsyncImage(
                                    model = activePhoto,
                                    contentDescription = currentProfile.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable {
                                            if (photos.size > 1) {
                                                selectedPhotoIndex = (selectedPhotoIndex + 1) % photos.size
                                            } else {
                                                previewPhotoUri = activePhoto
                                            }
                                        }
                                )
                            } else {
                                ProfileAvatarFallback(name = currentProfile.name)
                            }

                            // Top Gradient & Photo progress bars + Badges
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(104.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                                        )
                                    )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (photos.size > 1) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            photos.forEachIndexed { idx, _ ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(3.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(
                                                            if (idx == selectedPhotoIndex) Color.White
                                                            else Color.White.copy(alpha = 0.35f)
                                                        )
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Distance Pill
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color.Black.copy(alpha = 0.6f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.LocationOn,
                                                        contentDescription = null,
                                                        tint = CupidGold,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "${currentProfile.distanceKm} km",
                                                        color = Color.White,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }

                                            // Bio & Interest Compatibility Badge
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = RoseAccent.copy(alpha = 0.85f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.AutoAwesome,
                                                        contentDescription = null,
                                                        tint = CupidGold,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "%$compatibilityScore Uyum",
                                                        color = Color.White,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // VIP Button
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isVip) CupidGold.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.6f),
                                                border = if (isVip) BorderStroke(1.dp, CupidGold) else null,
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                IconButton(
                                                    onClick = onOpenVipPaywall,
                                                    modifier = Modifier.testTag("discover_vip_button")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.WorkspacePremium,
                                                        contentDescription = "VIP Üyelik",
                                                        tint = CupidGold,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            // Safety & Report Button
                                            Surface(
                                                shape = CircleShape,
                                                color = Color.Black.copy(alpha = 0.6f),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { onOpenReportDialog(currentProfile) },
                                                    modifier = Modifier.testTag("report_user_button")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Security,
                                                        contentDescription = "Güvenlik & Şikayet",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Dynamic Swipe Stamps (BEĞENDİM / GEÇ / SÜPER BEĞENİ)
                            if (likeAlpha > 0.08f) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = OnlineGreen.copy(alpha = 0.2f * likeAlpha),
                                    border = BorderStroke(3.dp, OnlineGreen.copy(alpha = likeAlpha)),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(top = 74.dp, start = 22.dp)
                                        .rotate(-14f)
                                        .graphicsLayer { alpha = likeAlpha }
                                ) {
                                    Text(
                                        text = "BEĞENDİM ❤️",
                                        color = OnlineGreen,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            if (passAlpha > 0.08f) {
                                val passColor = Color(0xFFEF4444)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = passColor.copy(alpha = 0.2f * passAlpha),
                                    border = BorderStroke(3.dp, passColor.copy(alpha = passAlpha)),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 74.dp, end = 22.dp)
                                        .rotate(14f)
                                        .graphicsLayer { alpha = passAlpha }
                                ) {
                                    Text(
                                        text = "GEÇ ✕",
                                        color = passColor,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            if (superLikeAlpha > 0.08f) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = CupidGold.copy(alpha = 0.22f * superLikeAlpha),
                                    border = BorderStroke(3.dp, CupidGold.copy(alpha = superLikeAlpha)),
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 92.dp)
                                        .graphicsLayer { alpha = superLikeAlpha }
                                ) {
                                    Text(
                                        text = "SÜPER BEĞENİ ⭐",
                                        color = CupidGold,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Bottom Gradient & Profile + Bio Sheet
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.86f),
                                                Color.Black.copy(alpha = 0.98f)
                                            )
                                        )
                                    )
                                    .padding(16.dp)
                            ) {
                                Column {
                                    // Name, Age, Verified & Quick Bio Like Button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "${currentProfile.name}, ${currentProfile.age}",
                                                fontSize = 26.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (currentProfile.isVerified) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = "Doğrulanmış Profil",
                                                    tint = VerifiedBlue,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        // Express direct interest in profile's bio
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = RoseAccent.copy(alpha = 0.22f),
                                            border = BorderStroke(1.dp, RoseAccent),
                                            modifier = Modifier
                                                .clickable {
                                                    performAnimatedSwipe(SwipeDirection.RIGHT, currentProfile)
                                                }
                                                .testTag("bio_like_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Favorite,
                                                    contentDescription = "Biyografiyi Beğen",
                                                    tint = RoseAccent,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Biyografiyi Beğen",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "${currentProfile.occupation} • ${currentProfile.city} • 🎯 ${currentProfile.lookingFor}",
                                        fontSize = 13.sp,
                                        color = Color(0xFFE2E8F0),
                                        fontWeight = FontWeight.Medium
                                    )

                                    // Visible Bio Preview directly on the swipe card
                                    if (currentProfile.bio.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = currentProfile.bio,
                                            fontSize = 13.5.sp,
                                            color = Color(0xFFF1F5F9),
                                            maxLines = if (isBioExpanded) Int.MAX_VALUE else 2,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 18.sp
                                        )
                                    }

                                    // Prompt quote preview on card
                                    if (!isBioExpanded && currentProfile.promptAnswer.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF2C2243).copy(alpha = 0.85f),
                                            border = BorderStroke(1.dp, CupidGold.copy(alpha = 0.35f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${currentProfile.promptQuestion}: ",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CupidGold,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "“${currentProfile.promptAnswer}”",
                                                    fontSize = 11.5.sp,
                                                    fontStyle = FontStyle.Italic,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Interests chips row (shared interests highlighted)
                                    FlowRow(
                                        maxItemsInEachRow = 4,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        currentProfile.interestList.take(4).forEach { interest ->
                                            val isShared = sharedInterests.any {
                                                it.equals(interest, ignoreCase = true)
                                            }
                                            InterestChip(
                                                text = if (isShared) "✨ $interest" else interest,
                                                isSelected = isShared
                                            )
                                        }
                                        if (currentProfile.interestList.size > 4) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = NightCard,
                                                modifier = Modifier.padding(4.dp)
                                            ) {
                                                Text(
                                                    text = "+${currentProfile.interestList.size - 4}",
                                                    fontSize = 12.sp,
                                                    color = Color.LightGray,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Expandable Bio & Prompt trigger
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isBioExpanded = !isBioExpanded }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Swipe,
                                                contentDescription = null,
                                                tint = CupidGold,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isBioExpanded) {
                                                    "Detaylı Bilgileri Gizle"
                                                } else {
                                                    "Detaylı Biyografi & Soru-Cevap (${sharedInterests.size} Ortak İlgi)"
                                                },
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CupidGold
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isBioExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = CupidGold
                                        )
                                    }

                                    AnimatedVisibility(visible = isBioExpanded) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(190.dp)
                                                .verticalScroll(rememberScrollState())
                                                .padding(top = 8.dp)
                                        ) {
                                            // Prompt Question & Answer
                                            Card(
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2243)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(
                                                        text = currentProfile.promptQuestion,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = CupidGold
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "“${currentProfile.promptAnswer}”",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color.White
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            // Details row (Eğitim, Boy, Burç, Yaşam)
                                            Text(
                                                text = "Kişisel Detaylar",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RoseAccent
                                            )
                                            Text(
                                                text = "🎓 ${currentProfile.education}\n📏 ${currentProfile.heightCm} cm • ${currentProfile.zodiac}\n🎯 ${currentProfile.lookingFor}",
                                                fontSize = 12.5.sp,
                                                color = Color(0xFFE2E8F0),
                                                lineHeight = 18.sp
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Lifestyle tags
                                            FlowRow(modifier = Modifier.fillMaxWidth()) {
                                                currentProfile.lifestyleList.forEach { tag ->
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = Color(0xFF1E1730),
                                                        modifier = Modifier.padding(2.dp)
                                                    ) {
                                                        Text(
                                                            text = tag,
                                                            fontSize = 11.5.sp,
                                                            color = Color(0xFFCBD5E1),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons Bar (Rewind, Pass, SuperLike, Like)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind / Undo Button
                    FloatingActionButton(
                        onClick = {
                            if (currentIndex > 0) {
                                currentIndex--
                                selectedPhotoIndex = 0
                                isBioExpanded = false
                            }
                        },
                        containerColor = NightCard,
                        contentColor = if (currentIndex > 0) CupidGold else Color.Gray,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("rewind_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Önceki Karta Dön",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Pass Button (X)
                    FloatingActionButton(
                        onClick = {
                            performAnimatedSwipe(SwipeDirection.LEFT, currentProfile)
                        },
                        containerColor = NightCard,
                        contentColor = Color(0xFFEF4444),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(58.dp)
                            .testTag("pass_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Geç",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Super Like Button (Star)
                    FloatingActionButton(
                        onClick = {
                            performAnimatedSwipe(SwipeDirection.UP, currentProfile)
                        },
                        containerColor = NightCard,
                        contentColor = CupidGold,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(50.dp)
                            .testTag("super_like_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Süper Beğeni",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Like Button (Heart)
                    FloatingActionButton(
                        onClick = {
                            performAnimatedSwipe(SwipeDirection.RIGHT, currentProfile)
                        },
                        containerColor = RoseAccent,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(64.dp)
                            .testTag("like_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Beğen",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // Create Moment / Discover Share Dialog (VIP only)
        if (showShareDialog && isVip) {
            CreateMomentDialog(
                onDismiss = { showShareDialog = false },
                onPublish = { caption, uri, type, location ->
                    onPublishMoment(caption, uri, type, location)
                    showShareDialog = false
                }
            )
        }

        // Full Screen Photo Preview Dialog if opened
        previewPhotoUri?.let { uri ->
            MediaViewerDialog(
                mediaUri = uri,
                mediaType = "IMAGE",
                title = currentProfile?.name ?: "",
                onDismiss = { previewPhotoUri = null }
            )
        }
    }
}

@Composable
private fun ProfileAvatarFallback(name: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF381A4E),
                        Color(0xFF201030),
                        Color(0xFF100719)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF261437),
                border = BorderStroke(3.dp, RoseAccent.copy(alpha = 0.5f)),
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Boş Profil Fotoğrafı",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(70.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = name,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

internal fun calculateCompatibilityPercent(
    currentUser: UserProfileEntity?,
    candidate: UserProfileEntity
): Int {
    if (currentUser == null) return 85
    val myInterests = currentUser.interestList.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
    val theirInterests = candidate.interestList.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

    val sharedInterestCount = myInterests.intersect(theirInterests).size
    val myLifestyle = currentUser.lifestyleList.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
    val theirLifestyle = candidate.lifestyleList.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
    val sharedLifestyleCount = myLifestyle.intersect(theirLifestyle).size

    val sameGoalBoost = if (
        currentUser.lookingFor.isNotBlank() &&
        currentUser.lookingFor.equals(candidate.lookingFor, ignoreCase = true)
    ) 10 else 0

    val bioScore = if (candidate.bio.length >= 40 && candidate.promptAnswer.isNotBlank()) 8 else 4
    val raw = 65 + (sharedInterestCount * 9) + (sharedLifestyleCount * 5) + sameGoalBoost + bioScore
    return raw.coerceIn(68, 99)
}
