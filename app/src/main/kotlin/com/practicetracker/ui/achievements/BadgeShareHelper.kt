package com.practicetracker.ui.achievements

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.practicetracker.data.datastore.UserProfile
import com.practicetracker.domain.model.Milestone
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object BadgeShareHelper {

    private const val CARD_W = 800
    private const val CARD_H = 500

    fun share(context: Context, milestone: Milestone, earnedAt: Instant, profile: UserProfile) {
        val bitmap = renderCard(milestone, earnedAt, profile)
        val file = saveBitmap(context, bitmap)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_TEXT,
                "I just earned the \"${milestone.displayName}\" badge in PracticeTracker! 🎵"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share badge"))
    }

    private fun renderCard(milestone: Milestone, earnedAt: Instant, profile: UserProfile): Bitmap {
        val badge = BadgeRegistry.get(milestone)
        val bitmap = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#1A1A2E")
        }
        canvas.drawRect(0f, 0f, CARD_W.toFloat(), CARD_H.toFloat(), bgPaint)

        // Accent card
        val accentColor = badge.color
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(
                40,
                (accentColor.red * 255).toInt(),
                (accentColor.green * 255).toInt(),
                (accentColor.blue * 255).toInt()
            )
        }
        canvas.drawRoundRect(RectF(32f, 32f, CARD_W - 32f, CARD_H - 32f), 32f, 32f, accentPaint)

        // Emoji icon (large)
        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 120f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(badge.emoji, CARD_W / 2f, 200f, emojiPaint)

        // Milestone name
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 56f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(milestone.displayName, CARD_W / 2f, 290f, titlePaint)

        // User info line
        val subtitleText = buildString {
            if (profile.displayName.isNotBlank()) append(profile.displayName)
            if (profile.instrument.isNotBlank()) {
                if (isNotEmpty()) append(" · ")
                append(profile.instrument)
            }
        }.ifBlank { "PracticeTracker" }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(180, 255, 255, 255)
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(subtitleText, CARD_W / 2f, 350f, subPaint)

        // Date
        val dateStr = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.LONG)
            .withZone(ZoneId.systemDefault())
            .format(earnedAt)
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(120, 255, 255, 255)
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(dateStr, CARD_W / 2f, 410f, datePaint)

        // App name watermark
        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(80, 255, 255, 255)
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("PracticeTracker", CARD_W / 2f, 460f, watermarkPaint)

        return bitmap
    }

    private fun saveBitmap(context: Context, bitmap: Bitmap): File {
        val file = File(context.cacheDir, "badge_share.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
}
