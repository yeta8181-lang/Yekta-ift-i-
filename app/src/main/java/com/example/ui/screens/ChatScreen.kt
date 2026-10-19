package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ChatMessageEntity
import com.example.data.model.UserProfileEntity
import com.example.ui.components.AvatarWithBadge
import com.example.ui.components.MediaViewerDialog
import com.example.ui.theme.CupidGold
import com.example.ui.theme.NightCard
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightSurface
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.RoseAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MatchesAndChatScreen(
    matches: List<UserProfileEntity>,
    allProfiles: List<UserProfileEntity>,
    isVip: Boolean = false,
    onSelectConversation: (UserProfileEntity) -> Unit,
    onOpenVipPaywall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NightSurface)
            .padding(top = 12.dp)
    ) {
        // Non-VIP Chat Restriction Banner
        if (!isVip) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NightCard),
                border = BorderStroke(1.5.dp, CupidGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("chat_vip_only_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = CupidGold.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = CupidGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sohbet Özelliği Sadece VIP Üyelere Özeldir 👑",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CupidGold
                            )
                            Text(
                                text = "Eşleştiğin kişilerle mesajlaşmak, fotoğraf ve video göndermek için VIP üyeliğe geç.",
                                fontSize = 11.5.sp,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onOpenVipPaywall,
                        colors = ButtonDefaults.buttonColors(containerColor = CupidGold),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("chat_unlock_vip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "VIP Üyeliğe Geç & Sohbetin Kilidini Aç",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }

        // Matches Stories Bar
        Text(
            text = "Yeni Eşleşmeler ✨",
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        if (matches.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = NightCard,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = RoseAccent)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Henüz eşleşmen yok",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Keşfet veya Harita üzerinden profilleri beğen!",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
            ) {
                items(matches) { match ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                if (isVip) {
                                    onSelectConversation(match)
                                } else {
                                    onOpenVipPaywall()
                                }
                            }
                            .testTag("match_bubble_${match.id}")
                    ) {
                        AvatarWithBadge(
                            imageUrl = match.photoList.firstOrNull() ?: "",
                            size = 64.dp,
                            isVerified = match.isVerified,
                            isOnline = match.isOnline
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = match.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Messages List Header
        Text(
            text = "Sohbetler 💬",
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        val conversations = remember(matches, allProfiles) {
            val list = matches.toMutableList()
            allProfiles.find { it.name == "Elif" }?.let { if (!list.contains(it)) list.add(0, it) }
            list
        }

        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Henüz aktif bir sohbet yok.\nBirisiyle eşleştiğinde burada görünecek!",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                items(conversations) { user ->
                    ConversationListItem(
                        user = user,
                        isVip = isVip,
                        onClick = {
                            if (isVip) {
                                onSelectConversation(user)
                            } else {
                                onOpenVipPaywall()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationListItem(
    user: UserProfileEntity,
    isVip: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NightCard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
            .testTag("conversation_${user.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarWithBadge(
                imageUrl = user.photoList.firstOrNull() ?: "",
                size = 52.dp,
                isVerified = user.isVerified,
                isOnline = user.isOnline
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = user.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (!isVip) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CupidGold.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, CupidGold)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = CupidGold,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "VIP",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CupidGold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (user.isOnline) "Çevrimiçi" else "Bugün",
                            fontSize = 11.sp,
                            color = if (user.isOnline) OnlineGreen else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (!isVip) {
                        "🔒 Sohbeti görüntülemek ve yazmak için VIP üye ol"
                    } else if (user.name == "Elif") {
                        "Evet atölyedeyim, birazdan sahil kafesine..."
                    } else {
                        "Yeni eşleşme! Sohbeti başlat ☕"
                    },
                    fontSize = 12.5.sp,
                    color = if (!isVip) CupidGold else Color(0xFFCBD5E1),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    user: UserProfileEntity,
    messages: List<ChatMessageEntity>,
    isVip: Boolean = true,
    onSendMessage: (text: String, mediaUri: String?, mediaType: String?) -> Unit,
    onBack: () -> Unit,
    onReportOrBlock: (UserProfileEntity) -> Unit,
    onOpenVipPaywall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var inputText by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var selectedMediaPreview by remember { mutableStateOf<Pair<String, String>?>(null) }
    val listState = rememberLazyListState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            if (isVip) {
                onSendMessage("", it.toString(), "IMAGE")
            } else {
                onOpenVipPaywall()
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            if (isVip) {
                onSendMessage("", it.toString(), "VIDEO")
            } else {
                onOpenVipPaywall()
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .background(NightSurface)
    ) {
        // Chat Top Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarWithBadge(
                        imageUrl = user.photoList.firstOrNull() ?: "",
                        size = 38.dp,
                        isVerified = user.isVerified,
                        isOnline = user.isOnline
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = user.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (user.isOnline) "Çevrimiçi" else "${user.distanceKm} km uzakta",
                            fontSize = 11.sp,
                            color = if (user.isOnline) OnlineGreen else Color.LightGray
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("chat_menu_button")
                ) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menü", tint = Color.White)
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(NightCard)
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = RoseAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Bildir ve Engelle", color = Color.White)
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onReportOrBlock(user)
                        }
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = NightCard)
        )

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.size <= 1) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = NightCard.copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, NightCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "💡 Buzları Kırıcı Öneriler:",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CupidGold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val prompts = listOf(
                                    "Kahve içmeye ne dersin? ☕",
                                    "Fotoğraftaki yer çok güzel, neresiydi? 📸",
                                    "Müzik tarzlarımız çok benziyor! 🎶"
                                )
                                prompts.forEach { prompt ->
                                    Text(
                                        text = prompt,
                                        fontSize = 12.sp,
                                        color = Color(0xFFE2E8F0),
                                        modifier = Modifier
                                            .clickable {
                                                if (isVip) {
                                                    onSendMessage(prompt, null, null)
                                                } else {
                                                    onOpenVipPaywall()
                                                }
                                            }
                                            .padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(messages) { msg ->
                val isMe = msg.senderId == "me"
                ChatBubble(
                    message = msg,
                    isMe = isMe,
                    onMediaClick = { uri, type ->
                        selectedMediaPreview = Pair(uri, type)
                    }
                )
            }
        }

        // Bottom Message Input Bar (VIP Gated)
        if (!isVip) {
            Surface(
                color = NightCard,
                border = BorderStroke(1.dp, CupidGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenVipPaywall() }
                    .testTag("chat_vip_lock_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CupidGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sadece VIP üyeler sohbet edebilir",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CupidGold
                    ) {
                        Text(
                            text = "VIP Ol 👑",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        } else {
            Surface(
                color = NightCard,
                border = BorderStroke(1.dp, NightCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Photo Attachment Button
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("chat_pick_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Fotoğraf Gönder",
                            tint = CupidGold
                        )
                    }

                    // Video Attachment Button
                    IconButton(
                        onClick = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier.testTag("chat_pick_video_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Gönder",
                            tint = RoseAccent
                        )
                    }

                    // Input Text Field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Bir şeyler yaz...", fontSize = 14.sp, color = Color.Gray) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(20.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = NightSurface,
                            unfocusedContainerColor = NightSurface,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = RoseAccent,
                            unfocusedIndicatorColor = NightCardBorder
                        ),
                        maxLines = 4
                    )

                    // Send Button
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText.trim(), null, null)
                                inputText = ""
                            }
                        },
                        modifier = Modifier.testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Gönder",
                            tint = if (inputText.isNotBlank()) RoseAccent else Color.Gray
                        )
                    }
                }
            }
        }

        // Full Screen Media Preview Dialog
        selectedMediaPreview?.let { (uri, type) ->
            MediaViewerDialog(
                mediaUri = uri,
                mediaType = type,
                title = "${user.name} ile Paylaşılan Medya",
                onDismiss = { selectedMediaPreview = null }
            )
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessageEntity,
    isMe: Boolean,
    onMediaClick: (uri: String, type: String) -> Unit
) {
    val bubbleColor = if (isMe) RoseAccent else Color(0xFF2C2243)
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val shape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                if (message.mediaUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onMediaClick(message.mediaUri, message.mediaType ?: "IMAGE")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = message.mediaUri,
                            contentDescription = "Ek Medya",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (message.mediaType == "VIDEO") {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.7f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Videoyu Oynat",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (message.text.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        fontSize = 14.sp,
                        color = Color.White,
                        lineHeight = 19.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 10.sp,
                        color = Color(0xFFE2E8F0).copy(alpha = 0.75f)
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Okundu",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}
