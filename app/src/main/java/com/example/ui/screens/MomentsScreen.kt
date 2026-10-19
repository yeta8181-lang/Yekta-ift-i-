package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MomentEntity
import com.example.ui.components.AvatarWithBadge
import com.example.ui.components.MediaViewerDialog
import com.example.ui.theme.CupidGold
import com.example.ui.theme.NightCard
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightSurface
import com.example.ui.theme.RoseAccent

@Composable
fun MomentsScreen(
    moments: List<MomentEntity>,
    isVip: Boolean = false,
    onToggleLike: (MomentEntity) -> Unit,
    onPublishMoment: (caption: String, mediaUri: String, mediaType: String, location: String) -> Unit,
    onOpenVipPaywall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedMediaPreview by remember { mutableStateOf<Pair<String, String>?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NightSurface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Keşfet Anlar & Hikayeler 📸",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Çevrendeki VIP üyelerin fotoğraf ve video paylaşımları",
                        fontSize = 12.5.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (isVip) {
                            showCreateDialog = true
                        } else {
                            onOpenVipPaywall()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isVip) RoseAccent else CupidGold
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("add_moment_top_button")
                ) {
                    Icon(
                        imageVector = if (isVip) Icons.Default.Add else Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = if (isVip) Color.White else Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isVip) "Paylaş" else "Paylaş (VIP)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isVip) Color.White else Color.Black
                    )
                }
            }

            // Non-VIP Restriction Banner
            if (!isVip) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CupidGold.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, CupidGold.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onOpenVipPaywall() }
                        .testTag("moments_vip_notice_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CupidGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Keşfette Paylaşım Sadece VIP Üyelere Özeldir 👑",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CupidGold
                            )
                            Text(
                                text = "Fotoğraf ve video paylaşarak keşfette öne çıkmak için VIP üyeliğe geçin.",
                                fontSize = 11.5.sp,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }

            // Moments Feed
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(moments) { moment ->
                    MomentFeedCard(
                        moment = moment,
                        onLikeClick = { onToggleLike(moment) },
                        onMediaClick = {
                            selectedMediaPreview = Pair(moment.mediaUri, moment.mediaType)
                        }
                    )
                }
            }
        }

        // New Moment Dialog (Only accessible when VIP)
        if (showCreateDialog && isVip) {
            CreateMomentDialog(
                onDismiss = { showCreateDialog = false },
                onPublish = { caption, uri, type, location ->
                    onPublishMoment(caption, uri, type, location)
                    showCreateDialog = false
                }
            )
        }

        // Media Fullscreen Viewer Dialog
        selectedMediaPreview?.let { (uri, type) ->
            MediaViewerDialog(
                mediaUri = uri,
                mediaType = type,
                title = "Paylaşılan An",
                onDismiss = { selectedMediaPreview = null }
            )
        }
    }
}

@Composable
private fun MomentFeedCard(
    moment: MomentEntity,
    onLikeClick: () -> Unit,
    onMediaClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NightCard),
        border = BorderStroke(1.dp, NightCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Author Avatar, Name, Location
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarWithBadge(
                    imageUrl = moment.authorAvatar,
                    size = 44.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = moment.authorName,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CupidGold.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, CupidGold.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "VIP",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CupidGold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = CupidGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = moment.location,
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Media (Photo or Video preview)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onMediaClick() },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = moment.mediaUri,
                    contentDescription = moment.caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (moment.mediaType == "VIDEO") {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Video Oynat",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Caption
            if (moment.caption.isNotBlank()) {
                Text(
                    text = moment.caption,
                    fontSize = 13.5.sp,
                    color = Color.White,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Actions: Like button & count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onLikeClick,
                    modifier = Modifier.testTag("moment_like_${moment.id}")
                ) {
                    Icon(
                        imageVector = if (moment.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Beğen",
                        tint = if (moment.isLiked) RoseAccent else Color.LightGray,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "${moment.likesCount} beğeni",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
    }
}

@Composable
internal fun CreateMomentDialog(
    onDismiss: () -> Unit,
    onPublish: (caption: String, uri: String, type: String, location: String) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Kadıköy, İstanbul") }
    var selectedUri by remember { mutableStateOf<String?>(null) }
    var mediaType by remember { mutableStateOf("IMAGE") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it.toString()
            mediaType = "IMAGE"
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it.toString()
            mediaType = "VIDEO"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = CupidGold,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Keşfette Yeni Paylaşım Yap",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Fotoğraf veya video seç:",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF372C54)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = CupidGold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fotoğraf", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            videoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF372C54)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = null, tint = RoseAccent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Video", fontSize = 12.sp)
                    }
                }

                // Selected Media Preview or sample fallback
                Spacer(modifier = Modifier.height(10.dp))
                val displayUri = selectedUri ?: "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800&auto=format&fit=crop&q=80"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = displayUri,
                        contentDescription = "Seçilen Medya",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Açıklama / Düşünceniz:") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Konum:") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalUri = selectedUri ?: "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800&auto=format&fit=crop&q=80"
                    onPublish(caption, finalUri, mediaType, location)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoseAccent),
                modifier = Modifier.testTag("publish_moment_confirm_button")
            ) {
                Text("Paylaş", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç", color = Color.Gray)
            }
        }
    )
}
