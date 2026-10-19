package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.ui.theme.CupidGold
import com.example.ui.theme.NightCard
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightSurface
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.RoseAccent
import java.io.File
import java.io.FileOutputStream

data class PlayStoreAssetSpec(
    val id: String,
    val fileName: String,
    val title: String,
    val playConsoleField: String,
    val widthPx: Int,
    val heightPx: Int,
    val aspectRatio: Float,
    val description: String
)

object PlayStoreGraphicsGenerator {

    val ASSET_SPECS = listOf(
        PlayStoreAssetSpec(
            id = "app_icon",
            fileName = "kalp_kalbe_icon_512x512.png",
            title = "1. Uygulama İkonu (512 × 512 px)",
            playConsoleField = "Play Console > Mağaza Girişi > Uygulama İkonu",
            widthPx = 512,
            heightPx = 512,
            aspectRatio = 1f,
            description = "Google Play Console için 512x512 piksel, 32-bit PNG yüksek çözünürlüklü Kalp Kalbe uygulama ikonu."
        ),
        PlayStoreAssetSpec(
            id = "feature_graphic",
            fileName = "kalp_kalbe_feature_graphic_1024x500.png",
            title = "2. Öne Çıkan Görsel (1024 × 500 px)",
            playConsoleField = "Play Console > Mağaza Girişi > Öne Çıkan Görsel (Feature Graphic)",
            widthPx = 1024,
            heightPx = 500,
            aspectRatio = 1024f / 500f,
            description = "Google Play mağaza üst vitrini için zorunlu 1024x500 piksel tanıtım afişi."
        ),
        PlayStoreAssetSpec(
            id = "phone_discover",
            fileName = "kalp_kalbe_phone_1_kesfet_1080x1920.png",
            title = "3. Telefon Ekran Görüntüsü 1: Keşfet & Uyum (1080 × 1920 px)",
            playConsoleField = "Play Console > Telefon Ekran Görüntüleri (9:16 Dikey)",
            widthPx = 1080,
            heightPx = 1920,
            aspectRatio = 9f / 16f,
            description = "Ortak ilgi alanı, biyografi ve % uyum oranına göre profil kaydırma ekranı."
        ),
        PlayStoreAssetSpec(
            id = "phone_radar",
            fileName = "kalp_kalbe_phone_2_radar_1080x1920.png",
            title = "4. Telefon Ekran Görüntüsü 2: 100 km Canlı Radar (1080 × 1920 px)",
            playConsoleField = "Play Console > Telefon Ekran Görüntüleri (9:16 Dikey)",
            widthPx = 1080,
            heightPx = 1920,
            aspectRatio = 9f / 16f,
            description = "Yakındaki gerçek profilleri mesafe filtreli canlı radar haritasında keşfetme ekranı."
        ),
        PlayStoreAssetSpec(
            id = "phone_chat",
            fileName = "kalp_kalbe_phone_3_sohbet_1080x1920.png",
            title = "5. Telefon Ekran Görüntüsü 3: VIP Sohbet & Anlar (1080 × 1920 px)",
            playConsoleField = "Play Console > Telefon Ekran Görüntüleri (9:16 Dikey)",
            widthPx = 1080,
            heightPx = 1920,
            aspectRatio = 9f / 16f,
            description = "VIP üyelere özel sınırsız mesajlaşma, fotoğraf/video gönderimi ve Keşfet paylaşımı ekranı."
        ),
        PlayStoreAssetSpec(
            id = "phone_vip",
            fileName = "kalp_kalbe_phone_4_vip_1080x1920.png",
            title = "6. Telefon Ekran Görüntüsü 4: VIP Abonelik Paketleri (1080 × 1920 px)",
            playConsoleField = "Play Console > Telefon Ekran Görüntüleri (9:16 Dikey)",
            widthPx = 1080,
            heightPx = 1920,
            aspectRatio = 9f / 16f,
            description = "Google Play güvenceli Haftalık, Aylık, Yıllık ve Ömür Boyu VIP üyelik yönetim ekranı."
        ),
        PlayStoreAssetSpec(
            id = "tablet_10inch",
            fileName = "kalp_kalbe_tablet_1920x1080.png",
            title = "7. 7 inç & 10 inç Tablet Ekran Görüntüsü (1920 × 1080 px)",
            playConsoleField = "Play Console > 7 inç ve 10 inç Tablet Ekran Görüntüleri (16:9 Yatay)",
            widthPx = 1920,
            heightPx = 1080,
            aspectRatio = 16f / 9f,
            description = "Google Play Console tablet incelemesi için 16:9 geniş ekran çoklu panel görünümü."
        )
    )

    fun generateBitmap(assetId: String): Bitmap {
        return when (assetId) {
            "app_icon" -> renderAppIcon512()
            "feature_graphic" -> renderFeatureGraphic1024x500()
            "phone_discover" -> renderPhoneScreenshot(
                stepBadge = "KEŞFET & EŞLEŞ",
                headline = "Ortak İlgi Alanı ve",
                subHeadline = "%96 Uyum ile Gerçek Aşk",
                screenType = 1
            )
            "phone_radar" -> renderPhoneScreenshot(
                stepBadge = "CANLI HARİTA & RADAR",
                headline = "100 km Çevrendeki",
                subHeadline = "Kişileri Anında Keşfet",
                screenType = 2
            )
            "phone_chat" -> renderPhoneScreenshot(
                stepBadge = "VIP ÖZEL SOHBET",
                headline = "Güvenli Mesajlaşma ve",
                subHeadline = "Keşfette Anlar Paylaşımı",
                screenType = 3
            )
            "phone_vip" -> renderPhoneScreenshot(
                stepBadge = "GOOGLE PLAY ABONELİK",
                headline = "Kalp Kalbe VIP ile",
                subHeadline = "Tüm Sınırları Kaldır 👑",
                screenType = 4
            )
            "tablet_10inch" -> renderTabletScreenshot1920x1080()
            else -> renderAppIcon512()
        }
    }

    private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val path = Path()
        val topCurveHeight = size * 0.3f
        path.moveTo(cx, cy + size * 0.35f)
        // Left side of heart
        path.cubicTo(
            cx - size * 0.55f, cy + size * 0.05f,
            cx - size * 0.55f, cy - topCurveHeight,
            cx, cy - topCurveHeight * 0.35f
        )
        // Right side of heart
        path.cubicTo(
            cx + size * 0.55f, cy - topCurveHeight,
            cx + size * 0.55f, cy + size * 0.05f,
            cx, cy + size * 0.35f
        )
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun renderAppIcon512(): Bitmap {
        val w = 512
        val h = 512
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Deep velvet gradient background (Full bleed, no rounded corners as required by Play Store 512x512 spec)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                intArrayOf(0xFF280E3B.toInt(), 0xFF130821.toInt(), 0xFF090412.toInt()),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Ambient glow behind hearts
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                256f, 256f, 220f,
                intArrayOf(0x66FF2E63, 0x22F59E0B, 0x00000000),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(256f, 256f, 220f, glowPaint)

        // Subtle radar rings
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = 0x33FF2E63
        }
        canvas.drawCircle(256f, 260f, 145f, ringPaint)
        ringPaint.color = 0x28F59E0B
        canvas.drawCircle(256f, 260f, 195f, ringPaint)

        // Gold Heart (Right-back)
        val goldHeartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                220f, 140f, 390f, 360f,
                intArrayOf(0xFFFDE047.toInt(), 0xFFF59E0B.toInt(), 0xFFD97706.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        drawHeart(canvas, 298f, 255f, 240f, goldHeartPaint)

        // Rose-Pink Heart (Left-front)
        val roseHeartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                120f, 140f, 320f, 380f,
                intArrayOf(0xFFFF6B95.toInt(), 0xFFFF2E63.toInt(), 0xFFBE123C.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        drawHeart(canvas, 222f, 268f, 265f, roseHeartPaint)

        // Golden Crown / Sparkle at top center
        val crownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFBBF24.toInt()
            style = Paint.Style.FILL
        }
        val crownPath = Path().apply {
            moveTo(216f, 128f)
            lineTo(206f, 92f)
            lineTo(234f, 110f)
            lineTo(256f, 80f)
            lineTo(278f, 110f)
            lineTo(306f, 92f)
            lineTo(296f, 128f)
            close()
        }
        canvas.drawPath(crownPath, crownPaint)

        return bmp
    }

    private fun renderFeatureGraphic1024x500(): Bitmap {
        val w = 1024
        val h = 500
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Rich night gradient background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                intArrayOf(0xFF2A0F3D.toInt(), 0xFF140923.toInt(), 0xFF0B0514.toInt()),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Decorative radar glow on right
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                760f, 250f, 260f,
                intArrayOf(0x55FF2E63, 0x22F59E0B, 0x00000000),
                floatArrayOf(0f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(760f, 250f, 260f, glowPaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = 0x33FF2E63
        }
        canvas.drawCircle(760f, 250f, 140f, ringPaint)
        canvas.drawCircle(760f, 250f, 210f, ringPaint)

        // Left Badge Pill: "TÜRKİYE'NİN YENİ NESİL TANIŞMA UYGULAMASI"
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33F59E0B
            style = Paint.Style.FILL
        }
        val pillStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF59E0B.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val pillRect = RectF(56f, 52f, 470f, 92f)
        canvas.drawRoundRect(pillRect, 20f, 20f, pillPaint)
        canvas.drawRoundRect(pillRect, 20f, 20f, pillStroke)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFBBF24.toInt()
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("👑  KALP KALBE • VIP SOHBET & CANLI RADAR", 76f, 78f, textPaint)

        // Main Brand Title: "Kalp Kalbe"
        textPaint.apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 68f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Kalp Kalbe", 56f, 174f, textPaint)

        // Subtitle
        textPaint.apply {
            color = 0xFFE2E8F0.toInt()
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("En yakınındaki samimi profilleri haritada keşfet,", 56f, 224f, textPaint)
        canvas.drawText("ortak ilgi alanlarına göre eşleş ve güvenle sohbet et.", 56f, 258f, textPaint)

        // Feature Chips on Left Bottom
        val features = listOf(
            "❤️ %96 Uyum Eşleşmesi",
            "📍 100 km Canlı Radar",
            "👑 VIP Sohbet & Anlar"
        )
        var chipX = 56f
        features.forEach { label ->
            val chipW = 205f
            val r = RectF(chipX, 300f, chipX + chipW, 346f)
            val cp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1F1630.toInt() }
            val cs = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFF2E63.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 1.8f
            }
            canvas.drawRoundRect(r, 14f, 14f, cp)
            canvas.drawRoundRect(r, 14f, 14f, cs)

            textPaint.apply {
                color = 0xFFFFFFFF.toInt()
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(label, chipX + 14f, 329f, textPaint)
            chipX += chipW + 14f
        }

        // Bottom Google Play Verified Tagline
        textPaint.apply {
            color = 0xFF94A3B8.toInt()
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("✓ Mavi Tik Doğrulamalı Profiller   •   ✓ Google Play Güvenceli VIP Üyelik", 56f, 420f, textPaint)

        // Right Showcase Card (Floating UI Card + Intertwined Hearts)
        val cardRect = RectF(710f, 64f, 968f, 436f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1B122B.toInt()
        }
        val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF59E0B.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, 32f, 32f, cardPaint)
        canvas.drawRoundRect(cardRect, 32f, 32f, cardBorder)

        // Hearts inside right showcase card
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF59E0B.toInt() }
        val rosePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF2E63.toInt() }
        drawHeart(canvas, 862f, 195f, 125f, goldPaint)
        drawHeart(canvas, 816f, 205f, 138f, rosePaint)

        // Match % Badge inside card
        val matchPill = RectF(746f, 285f, 932f, 328f)
        val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF10B981.toInt() }
        canvas.drawRoundRect(matchPill, 16f, 16f, greenPaint)
        textPaint.apply {
            color = 0xFF052E16.toInt()
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("✨ %96 Uyum Oranı", 839f, 313f, textPaint)

        textPaint.apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 22f
        }
        canvas.drawText("Elif, 25 ✓", 839f, 368f, textPaint)
        textPaint.apply {
            color = 0xFFFBBF24.toInt()
            textSize = 16f
        }
        canvas.drawText("📍 1.4 km Yakınında • VIP", 839f, 398f, textPaint)

        return bmp
    }

    private fun renderPhoneScreenshot(
        stepBadge: String,
        headline: String,
        subHeadline: String,
        screenType: Int
    ): Bitmap {
        val w = 1080
        val h = 1920
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Background gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                intArrayOf(0xFF280E3B.toInt(), 0xFF13091F.toInt(), 0xFF090410.toInt()),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Top Marketing Badge
        val badgeRect = RectF(310f, 76f, 770f, 134f)
        val badgeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FF2E63 }
        val badgeStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFF2E63.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(badgeRect, 29f, 29f, badgeFill)
        canvas.drawRoundRect(badgeRect, 29f, 29f, badgeStroke)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFBBF24.toInt()
            textSize = 26f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(stepBadge, 540f, 114f, textPaint)

        // Headline & Subheadline
        textPaint.apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 56f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(headline, 540f, 212f, textPaint)

        textPaint.apply {
            color = 0xFFFF2E63.toInt()
            textSize = 52f
        }
        canvas.drawText(subHeadline, 540f, 280f, textPaint)

        // App Mockup Container Frame
        val frameRect = RectF(74f, 340f, 1006f, 1830f)
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF140B22.toInt() }
        val frameBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF3B2754.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        canvas.drawRoundRect(frameRect, 54f, 54f, framePaint)
        canvas.drawRoundRect(frameRect, 54f, 54f, frameBorder)

        // Top App Bar inside frame
        textPaint.apply {
            textAlign = Paint.Align.LEFT
            color = 0xFFFFFFFF.toInt()
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("❤️ Kalp Kalbe", 124f, 422f, textPaint)

        val vipChip = RectF(710f, 382f, 956f, 436f)
        val goldFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF59E0B.toInt() }
        canvas.drawRoundRect(vipChip, 20f, 20f, goldFill)
        textPaint.apply {
            textAlign = Paint.Align.CENTER
            color = 0xFF000000.toInt()
            textSize = 23f
        }
        canvas.drawText("👑 VIP ÜYELİK", 833f, 417f, textPaint)

        when (screenType) {
            1 -> drawDiscoverMockupContent(canvas, textPaint)
            2 -> drawRadarMockupContent(canvas, textPaint)
            3 -> drawChatMockupContent(canvas, textPaint)
            4 -> drawVipMockupContent(canvas, textPaint)
        }

        // Bottom Navigation Bar inside frame
        val navRect = RectF(78f, 1710f, 1002f, 1826f)
        val navPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1E1430.toInt() }
        canvas.drawRoundRect(navRect, 44f, 44f, navPaint)

        val tabs = listOf("Keşfet", "Harita", "Anlar", "Mesajlar", "Profilim")
        tabs.forEachIndexed { idx, label ->
            val tx = 170f + idx * 185f
            val active = (idx + 1) == screenType || (screenType == 4 && idx == 4)
            textPaint.apply {
                textAlign = Paint.Align.CENTER
                color = if (active) 0xFFFF2E63.toInt() else 0xFF94A3B8.toInt()
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, if (active) Typeface.BOLD else Typeface.NORMAL)
            }
            canvas.drawText(label, tx, 1780f, textPaint)
        }

        return bmp
    }

    private fun drawDiscoverMockupContent(canvas: Canvas, textPaint: Paint) {
        // Filter chips row
        val filters = listOf("👑 Keşfette Paylaş", "Tümü", "✨ Kahve ☕", "✨ Müzik 🎵")
        var fx = 120f
        filters.forEachIndexed { index, s ->
            val w = 200f
            val r = RectF(fx, 462f, fx + w, 514f)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (index == 0) 0x33F59E0B else if (index == 1) 0xFFFF2E63.toInt() else 0xFF231738.toInt()
            }
            canvas.drawRoundRect(r, 18f, 18f, p)
            textPaint.apply {
                textAlign = Paint.Align.CENTER
                color = if (index == 0) 0xFFFBBF24.toInt() else 0xFFFFFFFF.toInt()
                textSize = 20f
            }
            canvas.drawText(s, fx + w / 2f, 495f, textPaint)
            fx += w + 14f
        }

        // Main Profile Swipe Card
        val cardRect = RectF(120f, 544f, 960f, 1480f)
        val cardGrad = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                120f, 544f, 120f, 1480f,
                intArrayOf(0xFF4A1D4A.toInt(), 0xFF28123B.toInt(), 0xFF12081E.toInt()),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(cardRect, 42f, 42f, cardGrad)

        // Decorative romantic silhouette heart in center of card
        val roseGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FF2E63 }
        drawHeart(canvas, 540f, 850f, 340f, roseGlow)

        // Match Compatibility Badge
        val compatRect = RectF(156f, 584f, 410f, 642f)
        val greenFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF10B981.toInt() }
        canvas.drawRoundRect(compatRect, 22f, 22f, greenFill)
        textPaint.apply {
            textAlign = Paint.Align.CENTER
            color = 0xFF052E16.toInt()
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("✨ %96 Uyum Oranı", 283f, 621f, textPaint)

        // Profile Details on Card Bottom
        textPaint.apply {
            textAlign = Paint.Align.LEFT
            color = 0xFFFFFFFF.toInt()
            textSize = 48f
        }
        canvas.drawText("Elif Yılmaz, 25  ✓", 164f, 1170f, textPaint)

        textPaint.apply {
            color = 0xFFFBBF24.toInt()
            textSize = 26f
        }
        canvas.drawText("📍 İstanbul • 1.8 km yakınında • 👑 VIP Üye", 164f, 1220f, textPaint)

        textPaint.apply {
            color = 0xFFE2E8F0.toInt()
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("\"Kahve kokusu, canlı konserler ve spontane hafta sonu gezileri...\"", 164f, 1278f, textPaint)

        // Interest tags on card
        val tags = listOf("✨ Kahve ☕", "✨ Müzik 🎵", "✨ Seyahat ✈️", "Fotoğraf 📷")
        var tx = 164f
        tags.forEach { t ->
            val tw = 180f
            val tr = RectF(tx, 1320f, tx + tw, 1374f)
            val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x44FF2E63 }
            canvas.drawRoundRect(tr, 18f, 18f, tp)
            textPaint.apply {
                textAlign = Paint.Align.CENTER
                color = 0xFFFFFFFF.toInt()
                textSize = 21f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(t, tx + tw / 2f, 1354f, textPaint)
            tx += tw + 14f
        }

        // Swipe Action Buttons Row
        val btnColors = listOf(0xFFF59E0B.toInt(), 0xFFEF4444.toInt(), 0xFF38BDF8.toInt(), 0xFFFF2E63.toInt())
        val btnLabels = listOf("↺", "✕", "★", "❤️")
        val btnCenters = listOf(270f, 445f, 635f, 810f)
        btnCenters.forEachIndexed { idx, cx ->
            val cy = 1590f
            val radius = if (idx == 1 || idx == 3) 62f else 50f
            val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = btnColors[idx] }
            canvas.drawCircle(cx, cy, radius, bp)
            textPaint.apply {
                textAlign = Paint.Align.CENTER
                color = 0xFFFFFFFF.toInt()
                textSize = 40f
            }
            canvas.drawText(btnLabels[idx], cx, cy + 14f, textPaint)
        }
    }

    private fun drawRadarMockupContent(canvas: Canvas, textPaint: Paint) {
        // Radar Map Box
        val mapRect = RectF(120f, 470f, 960f, 1270f)
        val mapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF101929.toInt() }
        canvas.drawRoundRect(mapRect, 38f, 38f, mapPaint)

        val cx = 540f
        val cy = 870f
        val radarGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, 350f,
                intArrayOf(0x55FF2E63, 0x22F59E0B, 0x00000000),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, 350f, radarGlow)

        val ringStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = 0x66FF2E63
        }
        canvas.drawCircle(cx, cy, 120f, ringStroke)
        canvas.drawCircle(cx, cy, 230f, ringStroke)
        canvas.drawCircle(cx, cy, 340f, ringStroke)

        // Center "Sen" pin
        val mePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF2E63.toInt() }
        canvas.drawCircle(cx, cy, 36f, mePaint)
        textPaint.apply {
            textAlign = Paint.Align.CENTER
            color = 0xFFFFFFFF.toInt()
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("SEN", cx, cy + 8f, textPaint)

        // Nearby User Pins
        val pins = listOf(
            Triple(380f, 700f, "Zeynep • 1.2 km"),
            Triple(730f, 740f, "Selin • 2.8 km"),
            Triple(660f, 1040f, "Merve • 4.5 km"),
            Triple(340f, 1020f, "Defne • 6.1 km")
        )
        pins.forEach { (px, py, label) ->
            val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF59E0B.toInt() }
            canvas.drawCircle(px, py, 28f, pinPaint)
            val pill = RectF(px - 95f, py + 34f, px + 95f, py + 72f)
            val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCC1E1430.toInt() }
            canvas.drawRoundRect(pill, 12f, 12f, pillBg)
            textPaint.apply {
                color = 0xFFFFFFFF.toInt()
                textSize = 18f
            }
            canvas.drawText(label, px, py + 59f, textPaint)
        }

        // Bottom Selected Radar User Card
        val previewCard = RectF(120f, 1310f, 960f, 1660f)
        val cardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF211535.toInt() }
        canvas.drawRoundRect(previewCard, 32f, 32f, cardBg)

        textPaint.apply {
            textAlign = Paint.Align.LEFT
            color = 0xFFFBBF24.toInt()
            textSize = 24f
        }
        canvas.drawText("📡 CANLI GPS RADAR • 100 KM VIP MENZİL AKTİF", 160f, 1370f, textPaint)

        textPaint.apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 36f
        }
        canvas.drawText("Zeynep Kaya, 24 ✓  —  1.2 km Uzaklıkta", 160f, 1435f, textPaint)

        val chatBtn = RectF(160f, 1500f, 530f, 1610f)
        val likeBtn = RectF(560f, 1500f, 920f, 1610f)
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF59E0B.toInt() }
        val rosePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF2E63.toInt() }
        canvas.drawRoundRect(chatBtn, 24f, 24f, goldPaint)
        canvas.drawRoundRect(likeBtn, 24f, 24f, rosePaint)

        textPaint.apply {
            textAlign = Paint.Align.CENTER
            color = 0xFF000000.toInt()
            textSize = 26f
        }
        canvas.drawText("💬 Sohbet Et (VIP 👑)", 345f, 1565f, textPaint)
        textPaint.color = 0xFFFFFFFF.toInt()
        canvas.drawText("❤️ Profili Beğen", 740f, 1565f, textPaint)
    }

    private fun drawChatMockupContent(canvas: Canvas, textPaint: Paint) {
        // Mutual Matches Row
        textPaint.apply {
            textAlign = Paint.Align.LEFT
            color = 0xFFFBBF24.toInt()
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("👑 YENİ EŞLEŞMELER & VIP MESAJLAR", 124f, 495f, textPaint)

        // Chat Messages Bubbles
        val msg1 = RectF(124f, 550f, 760f, 680f)
        val msg2 = RectF(320f, 720f, 956f, 850f)
        val msg3 = RectF(124f, 890f, 800f, 1060f)

        val bubbleLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF25183B.toInt() }
        val bubbleRight = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF2E63.toInt() }

        canvas.drawRoundRect(msg1, 28f, 28f, bubbleLeft)
        canvas.drawRoundRect(msg2, 28f, 28f, bubbleRight)
        canvas.drawRoundRect(msg3, 28f, 28f, bubbleLeft)

        textPaint.apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Selam! Ortak ilgi alanlarımızda kahve ve konser var ☕🎵", 156f, 625f, textPaint)
        canvas.drawText("Merhaba Elif! Evet, bu hafta sonu Kadıköy'de harika bir etkinlik var ✨", 352f, 795f, textPaint)
        canvas.drawText("📸 [Fotoğraf Paylaşıldı] Harika olur, birlikte katılalım mı? ❤️", 156f, 985f, textPaint)

        // Discover & Moments VIP Share Showcase Card
        val momentCard = RectF(124f, 1130f, 956f, 1650f)
        val momentBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1F1433.toInt() }
        val momentStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF59E0B.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(momentCard, 34f, 34f, momentBg)
        canvas.drawRoundRect(momentCard, 34f, 34f, momentStroke)

        textPaint.apply {
            color = 0xFFFBBF24.toInt()
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("✨ KEŞFET & ANLAR'DA HİKAYE PAYLAŞIMI (VIP ÖZEL)", 164f, 1200f, textPaint)

        textPaint.apply {
            color = 0xFFE2E8F0.toInt()
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("• Fotoğraf ve videolarını Keşfet vitrininde paylaş", 164f, 1270f, textPaint)
        canvas.drawText("• Sınırsız mesaj, sesli/görsel medya gönderimi", 164f, 1325f, textPaint)
        canvas.drawText("• Mavi Tik doğrulamalı gerçek profillerle güvenli iletişim", 164f, 1380f, textPaint)

        val shareBtn = RectF(164f, 1460f, 916f, 1590f)
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF59E0B.toInt() }
        canvas.drawRoundRect(shareBtn, 26f, 26f, goldPaint)
        textPaint.apply {
            textAlign = Paint.Align.CENTER
            color = 0xFF000000.toInt()
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("📸 Keşfette Fotoğraf & Video Paylaş (VIP 👑)", 540f, 1536f, textPaint)
    }

    private fun drawVipMockupContent(canvas: Canvas, textPaint: Paint) {
        val plans = listOf(
            Triple("Haftalık VIP Başlangıç", "25,00 ₺ / Hafta", "Hızlı Deneme • 7 Gün"),
            Triple("Aylık VIP Üyelik (EN POPÜLER)", "50,00 ₺ / Ay", "Sınırsız Sohbet & Keşfet Paylaşımı"),
            Triple("Yıllık VIP Avantaj (%33 İNDİRİM)", "399,00 ₺ / Yıl", "12 Ay Kesintisiz Tüm VIP Özellikler"),
            Triple("Ömür Boyu VIP Paket", "499,00 ₺", "Tek Seferlik Ödeme • Kalıcı VIP")
        )

        var topY = 485f
        plans.forEachIndexed { index, (title, price, desc) ->
            val rect = RectF(124f, topY, 956f, topY + 215f)
            val isSelected = index == 1
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isSelected) 0xFF2E1F42.toInt() else 0xFF1D132E.toInt()
            }
            val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isSelected) 0xFFF59E0B.toInt() else 0xFF3B2754.toInt()
                style = Paint.Style.STROKE
                strokeWidth = if (isSelected) 4f else 2f
            }
            canvas.drawRoundRect(rect, 28f, 28f, bg)
            canvas.drawRoundRect(rect, 28f, 28f, border)

            textPaint.apply {
                textAlign = Paint.Align.LEFT
                color = 0xFFFFFFFF.toInt()
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(title, 164f, topY + 80f, textPaint)

            textPaint.apply {
                color = 0xFFCBD5E1.toInt()
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            canvas.drawText(desc, 164f, topY + 140f, textPaint)

            val pricePill = RectF(690f, topY + 60f, 920f, topY + 145f)
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isSelected) 0xFFF59E0B.toInt() else 0xFF2B1C40.toInt()
            }
            canvas.drawRoundRect(pricePill, 20f, 20f, pillPaint)
            textPaint.apply {
                textAlign = Paint.Align.CENTER
                color = if (isSelected) 0xFF000000.toInt() else 0xFFFBBF24.toInt()
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(price, 805f, topY + 112f, textPaint)

            topY += 240f
        }

        // Google Play Subscribe CTA
        val buyBtn = RectF(124f, 1480f, 956f, 1620f)
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF59E0B.toInt() }
        canvas.drawRoundRect(buyBtn, 36f, 36f, goldPaint)

        textPaint.apply {
            textAlign = Paint.Align.CENTER
            color = 0xFF000000.toInt()
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("👑 Google Play ile Abone Ol (50,00 ₺ / Ay)", 540f, 1562f, textPaint)
    }

    private fun renderTabletScreenshot1920x1080(): Bitmap {
        val w = 1920
        val h = 1080
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                intArrayOf(0xFF280E3B.toInt(), 0xFF13091F.toInt(), 0xFF090410.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("❤️ Kalp Kalbe  •  Keşfet, 100 km Canlı Radar ve VIP Sohbet", 80f, 96f, textPaint)

        // Left Pane: Discover Card
        val leftPane = RectF(80f, 140f, 920f, 990f)
        val panePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1C122C.toInt() }
        val paneStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFF2E63.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(leftPane, 36f, 36f, panePaint)
        canvas.drawRoundRect(leftPane, 36f, 36f, paneStroke)

        val rosePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF2E63.toInt() }
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF59E0B.toInt() }
        drawHeart(canvas, 500f, 450f, 240f, rosePaint)

        textPaint.apply {
            textAlign = Paint.Align.CENTER
            color = 0xFFFFFFFF.toInt()
            textSize = 42f
        }
        canvas.drawText("Elif Yılmaz, 25 ✓  •  %96 Uyum", 500f, 700f, textPaint)
        textPaint.apply {
            color = 0xFFFBBF24.toInt()
            textSize = 28f
        }
        canvas.drawText("📍 İstanbul • Ortak İlgi: Kahve ☕, Müzik 🎵, Seyahat ✈️", 500f, 760f, textPaint)

        // Right Pane: Radar + VIP Chat
        val rightPane = RectF(970f, 140f, 1840f, 990f)
        canvas.drawRoundRect(rightPane, 36f, 36f, panePaint)
        val goldStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF59E0B.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(rightPane, 36f, 36f, goldStroke)

        val rcx = 1405f
        val rcy = 440f
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = 0x55FF2E63
        }
        canvas.drawCircle(rcx, rcy, 110f, ringPaint)
        canvas.drawCircle(rcx, rcy, 210f, ringPaint)
        canvas.drawCircle(rcx, rcy, 32f, goldPaint)

        textPaint.apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 36f
        }
        canvas.drawText("📡 100 km Canlı Radar & VIP Sohbet 👑", rcx, 740f, textPaint)

        val ctaRect = RectF(1070f, 820f, 1740f, 930f)
        canvas.drawRoundRect(ctaRect, 28f, 28f, goldPaint)
        textPaint.apply {
            color = 0xFF000000.toInt()
            textSize = 30f
        }
        canvas.drawText("Google Play ile VIP Üye Ol • Sınırsız Sohbet", rcx, 886f, textPaint)

        return bmp
    }

    fun saveAssetToGallery(context: Context, spec: PlayStoreAssetSpec): Boolean {
        return try {
            val bitmap = generateBitmap(spec.id)
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, spec.fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KalpKalbePlayStore")
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun shareAssetImage(context: Context, spec: PlayStoreAssetSpec) {
        try {
            val bitmap = generateBitmap(spec.id)
            val cacheDir = File(context.cacheDir, "play_store_exports").apply { mkdirs() }
            val outFile = File(cacheDir, spec.fileName)
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            saveAssetToGallery(context, spec)
            Toast.makeText(
                context,
                "${spec.fileName} (${spec.widthPx}x${spec.heightPx}) galeriye kaydedildi!",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Görsel kaydedilirken hata oluştu.", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayStoreAssetsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val assets = remember { PlayStoreGraphicsGenerator.ASSET_SPECS }
    val bitmaps = remember {
        assets.associate { spec -> spec.id to PlayStoreGraphicsGenerator.generateBitmap(spec.id) }
    }
    var previewSpec by remember { mutableStateOf<PlayStoreAssetSpec?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NightSurface)
            .testTag("play_store_assets_screen")
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = CupidGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google Play Mağaza Görselleri",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("play_assets_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = NightCard)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Save All Banner
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1532)),
                border = BorderStroke(1.5.dp, CupidGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Kalp Kalbe — Resmi Google Play Yayın Görselleri",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Google Play Console'un zorunlu tuttuğu 512×512 Uygulama İkonu, 1024×500 Öne Çıkan Görsel, 1080×1920 Telefon Ekran Görüntüleri ve 1920×1080 Tablet görselleri birebir piksel ölçülerinde oluşturulmuştur.",
                        fontSize = 12.5.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            var savedCount = 0
                            assets.forEach { spec ->
                                if (PlayStoreGraphicsGenerator.saveAssetToGallery(context, spec)) {
                                    savedCount++
                                }
                            }
                            Toast.makeText(
                                context,
                                "Tüm $savedCount Google Play görseli Galeri > KalpKalbePlayStore klasörüne kaydedildi! ✨",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CupidGold),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("download_all_play_assets_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tüm Görselleri Galeriye Kaydet (7 Adet PNG)",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }

            assets.forEach { spec ->
                val bmp = bitmaps[spec.id]
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NightCard),
                    border = BorderStroke(1.dp, NightCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = spec.title,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = spec.playConsoleField,
                                    fontSize = 11.5.sp,
                                    color = CupidGold,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = OnlineGreen.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, OnlineGreen.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "${spec.widthPx}×${spec.heightPx} PNG",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnlineGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (bmp != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (spec.aspectRatio < 1f) {
                                            Modifier.height(340.dp)
                                        } else {
                                            Modifier.aspectRatio(spec.aspectRatio)
                                        }
                                    )
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF090410))
                                    .clickable { previewSpec = spec },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = spec.title,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = spec.description,
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { previewSpec = spec },
                                border = BorderStroke(1.dp, NightCardBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tam Ekran",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    PlayStoreGraphicsGenerator.shareAssetImage(context, spec)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RoseAccent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Kaydet (${spec.widthPx}×${spec.heightPx})",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    previewSpec?.let { spec ->
        val bmp = bitmaps[spec.id]
        Dialog(
            onDismissRequest = { previewSpec = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = spec.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                IconButton(
                    onClick = { previewSpec = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(NightCard, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
