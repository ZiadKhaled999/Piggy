package com.oryno.piggy_ledger.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.StreakManager
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Calendar

object StreakShareHelper {

    fun createStreakImageBitmap(context: Context, streakCount: Int): Bitmap {
        val width = 1080
        val height = 1280
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Outer Canvas Background (Soft Neutral)
        canvas.drawColor(Color.parseColor("#F8FAFC"))

        // Main White Card with Rounded Corners
        val cardMargin = 50f
        val cardRect = RectF(cardMargin, cardMargin, width - cardMargin, height - cardMargin)
        val cardRadius = 60f

        // Card Shadow/Glow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10F43F5E")
            style = Paint.Style.FILL
        }
        val shadowRect = RectF(cardRect.left - 10f, cardRect.top - 10f, cardRect.right + 10f, cardRect.bottom + 10f)
        canvas.drawRoundRect(shadowRect, cardRadius + 10f, cardRadius + 10f, shadowPaint)

        // Card Fill
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardPaint)

        // Card Border (Soft Pink)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FBCFE8")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, strokePaint)

        // Subtle Pink Grid Pattern on Card
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FCE7F3")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val gridSize = 80f
        var gx = cardRect.left + gridSize
        while (gx < cardRect.right) {
            canvas.drawLine(gx, cardRect.top, gx, cardRect.bottom, gridPaint)
            gx += gridSize
        }
        var gy = cardRect.top + gridSize
        while (gy < cardRect.bottom) {
            canvas.drawLine(cardRect.left, gy, cardRect.right, gy, gridPaint)
            gy += gridSize
        }

        // Header Text "Piggy Ledger"
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Piggy Ledger", width / 2f, cardRect.top + 110f, headerPaint)

        // Center Radial Pink Glow behind Mascot
        val glowCenterX = width / 2f
        val glowCenterY = cardRect.top + 410f
        val glowRadius = 400f
        val glowShader = RadialGradient(
            glowCenterX,
            glowCenterY,
            glowRadius,
            intArrayOf(
                Color.parseColor("#80F43F5E"),
                Color.parseColor("#30FB7185"),
                Color.parseColor("#00FFFFFF")
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = glowShader
        }
        canvas.drawCircle(glowCenterX, glowCenterY, glowRadius, glowPaint)

        // Draw Sparkle Accents in Pink
        drawSparkle(canvas, glowCenterX - 340f, glowCenterY - 160f, 36f, Color.parseColor("#F43F5E"))
        drawSparkle(canvas, glowCenterX + 330f, glowCenterY - 130f, 32f, Color.parseColor("#FB7185"))
        drawSparkle(canvas, glowCenterX - 310f, glowCenterY + 180f, 40f, Color.parseColor("#F43F5E"))
        drawSparkle(canvas, glowCenterX + 340f, glowCenterY + 160f, 34f, Color.parseColor("#FB7185"))

        // Draw Piggy Mascot Logo Image (Official App Logo - Extra Large)
        val mascotDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.img_app_logo)
            ?: ContextCompat.getDrawable(context, R.drawable.streak)
        if (mascotDrawable != null) {
            val mascotSize = 540
            val left = ((width - mascotSize) / 2)
            val top = (glowCenterY - mascotSize / 2f).toInt() - 10
            mascotDrawable.setBounds(left, top, left + mascotSize, top + mascotSize)
            mascotDrawable.draw(canvas)
        }

        // Draw Streak Number in Giant Pink Bold 3D Font
        val numberText = "$streakCount"
        val numberY = cardRect.top + 770f

        // 3D Shadow for Number
        val numberShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9F1239")
            textSize = 190f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(numberText, width / 2f + 8f, numberY + 8f, numberShadowPaint)

        // Main Number Fill (Bright Pink Gradient)
        val numberShader = LinearGradient(
            width / 2f, numberY - 150f, width / 2f, numberY,
            Color.parseColor("#FB7185"),
            Color.parseColor("#E11D48"),
            Shader.TileMode.CLAMP
        )
        val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = numberShader
            textSize = 190f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(numberText, width / 2f, numberY, numberPaint)

        // Subtitle Text: "day saving streak"
        val subtitleText = if (streakCount == 1) "day saving streak" else "days saving streak"
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#BE185D")
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(subtitleText, width / 2f, numberY + 70f, subtitlePaint)

        // Fetch Statement from JSON
        val statementText = getShareStatement(context, streakCount)

        // Statement Text Box
        val statementPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // Multiline wrapping for statement
        val maxWidth = cardRect.width() - 120f
        val lines = wrapText(statementText, statementPaint, maxWidth)
        var lineY = numberY + 180f
        val lineHeight = 54f

        for (line in lines) {
            canvas.drawText(line, width / 2f, lineY, statementPaint)
            lineY += lineHeight
        }

        return bitmap
    }

    fun saveImageToGallery(context: Context, bitmap: Bitmap): Boolean {
        val filename = "Piggy_Streak_${System.currentTimeMillis()}.png"
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/PiggyLedger")
                }
                val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                    true
                } else false
            } else {
                val imagesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                val piggyDir = File(imagesDir, "PiggyLedger").apply { mkdirs() }
                val file = File(piggyDir, filename)
                FileOutputStream(file).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun shareNativeImage(context: Context, bitmap: Bitmap) {
        try {
            val file = File(context.cacheDir, "shared_streak.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share your streak"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareToMessages(context: Context, bitmap: Bitmap) {
        try {
            val file = File(context.cacheDir, "shared_streak.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val smsIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra("sms_body", "Check out my savings streak on Piggy Ledger! 🐷🔥")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(smsIntent, "Share via Messages"))
        } catch (e: Exception) {
            shareNativeImage(context, bitmap)
        }
    }

    private fun drawSparkle(canvas: Canvas, cx: Float, cy: Float, size: Float, colorInt: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInt
            style = Paint.Style.FILL
        }
        val path = Path()
        path.moveTo(cx, cy - size)
        path.quadTo(cx, cy, cx + size, cy)
        path.quadTo(cx, cy, cx, cy + size)
        path.quadTo(cx, cy, cx - size, cy)
        path.quadTo(cx, cy, cx, cy - size)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun getShareStatement(context: Context, streakCount: Int): String {
        return try {
            val inputStream: InputStream = context.assets.open("piggy_streak_messages.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val categoriesArray = jsonObject.getJSONArray("categories")

            val userPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val userName = userPrefs.getString("auth_user_name", "")?.takeIf { it.isNotBlank() } ?: "Saver"

            val targetCategoryId = if (streakCount > 0) 8 else 10 // Category 8: Streak Extended / Logged Today, 10: Ghosted/Inactive
            var candidates = mutableListOf<String>()

            for (i in 0 until categoriesArray.length()) {
                val catObj = categoriesArray.getJSONObject(i)
                if (catObj.optInt("id") == targetCategoryId) {
                    val itemsArr = catObj.getJSONArray("items")
                    for (j in 0 until itemsArr.length()) {
                        candidates.add(itemsArr.getString(j))
                    }
                    break
                }
            }

            if (candidates.isEmpty()) {
                if (streakCount > 0) "Great job keeping your ledger updated!" else "Start your savings streak today!"
            } else {
                val selected = candidates[streakCount % candidates.size]
                selected
                    .replace("[Username]", userName)
                    .replace("[USER_NAME]", userName)
                    .replace("[Number]", streakCount.toString())
                    .replace("[Course]", "Budget")
            }
        } catch (e: Exception) {
            if (streakCount > 0) "Great job keeping your ledger updated!" else "Start your savings streak today!"
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            if (width <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }
}
